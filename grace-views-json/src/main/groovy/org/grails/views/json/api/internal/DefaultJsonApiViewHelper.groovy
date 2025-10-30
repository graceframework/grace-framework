/*
 * Copyright 2015-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.views.json.api.internal

import groovy.json.JsonGenerator
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.http.HttpMethod
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

import grails.rest.Link
import grails.views.api.http.Parameters

import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.model.types.EmbeddedCollection
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.views.json.api.JsonWritable
import org.grails.views.json.api.GrailsJsonViewHelper
import org.grails.views.json.api.JsonApiViewHelper
import org.grails.views.json.api.JsonView
import org.grails.views.json.api.jsonapi.JsonApiIdRenderStrategy
import org.grails.views.json.util.JsonToken

/**
 * @author Colin Harrington
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
class DefaultJsonApiViewHelper extends DefaultJsonViewHelper implements JsonApiViewHelper {

    /**
     * The jsonapiobject parameter
     */
    static final String JSON_API_OBJECT = 'jsonApiObject'

    /**
     * The meta parameter
     */
    static final String META = 'meta'

    /**
     * The pagination parameter
     */
    static final String PAGINATION = 'pagination'

    final GrailsJsonViewHelper viewHelper

    DefaultJsonApiViewHelper(JsonView view, GrailsJsonViewHelper viewHelper) {
        super(view)
        this.viewHelper = viewHelper
    }

    @Override
    JsonWritable render(Object object) {
        return render(object, [:])
    }

    @Override
    JsonWritable render(Object object, Map arguments) {
        if (object == null) {
            return JsonWritable.NULL_OUTPUT
        }
        JsonWritable jsonWritable = new JsonWritable() {

            @Override
            @CompileStatic
            Writer writeTo(Writer out) throws IOException {
                out.write(JsonToken.OPEN_BRACE)
                def meta = arguments.get(META)
                if (arguments.get(JSON_API_OBJECT)) {
                    renderJsonApiMember(out, meta)
                    out.write(JsonToken.COMMA)
                } else if (meta != null) {
                    renderMetaObject(out, meta)
                    out.write(JsonToken.COMMA)
                }
                if (object instanceof Throwable) {
                    renderException(out, object)
                } else if (objectHasErrors(object)) {
                    renderErrors(object).writeTo(out)
                } else {
                    renderData(object, arguments).writeTo(out)
                    out.write(JsonToken.COMMA)
                    renderLinks(object, arguments).writeTo(out)
                    renderIncluded(object, arguments).writeTo(out)
                }
                out.write(JsonToken.CLOSE_BRACE)
                return out
            }

        }
        return jsonWritable
    }

    boolean objectHasErrors(Object subject) {
        if (subject.hasProperty('errors')) {
            Object errors = subject.getAt('errors')
            if (errors instanceof Errors) {
                return errors.hasErrors()
            } else {
                return errors.asBoolean()
            }
        }
        return false
    }

    private boolean isAttributeAssociation(Association a) {
        a.embedded || a instanceof Basic
    }

    List<Association> getRelationships(PersistentEntity entity) {
        entity.associations.findAll { Association a ->
            !isAttributeAssociation(a)
        }
    }

    List<PersistentProperty> getAttributes(PersistentEntity entity) {
        entity.persistentProperties.findAll { PersistentProperty p ->
            if (p instanceof Association) {
                isAttributeAssociation((Association) p)
            } else {
                true
            }
        }
    }

    private void writeKey(Writer out, Object key) {
        out.write(generator.toJson(key))
        out.write(JsonToken.COLON)
    }

    private void writeKeyValue(Writer out, Object key, Object value) {
        out.write(generator.toJson(key))
        out.write(JsonToken.COLON)
        out.write(generator.toJson(value))
    }

    private void renderResource(Object object, Writer out) {
        renderResource(object, out, [:], '')
    }

    private void renderResource(Object object, Writer out, Map arguments, String basePath) {
        PersistentEntity entity = findEntity(object)

        if (entity == null) {
            throw new IllegalArgumentException('Rendering non persistent entities is not supported')
        }

        List<String> includes = getIncludes(arguments)
        List<String> excludes = getExcludes(arguments)
        boolean includeAssociations = includeAssociations(arguments)

        out.write(JsonToken.OPEN_BRACE)

        writeKeyValue(out, 'type', entity.decapitalizedName)

        PersistentProperty identity = entity.identity
        String idName = identity?.name

        if (idName != null) {
            out.write(JsonToken.COMMA)
            writeKeyValue(out, 'id', idGenerator.render(object, identity))
        }

        if (entity.persistentProperties) {
            List<PersistentProperty> attributes = getAttributes(entity)
            List<Association> relationships = getRelationships(entity)

            if (attributes) {
                out.write(JsonToken.COMMA)
                out.write(generator.toJson('attributes'))
                out.write(JsonToken.COLON)
                out.write(JsonToken.OPEN_BRACE)

                boolean firstAttribute = true
                for (persistentProperty in attributes) {
                    if (!includeExcludeSupport.shouldInclude(includes, excludes, "${basePath}${persistentProperty.name}".toString())) {
                        continue
                    }

                    if (!firstAttribute) {
                        out.write(JsonToken.COMMA)
                    }

                    out.write(generator.toJson(persistentProperty.name))
                    out.write(JsonToken.COLON)

                    Object prop = ((GroovyObject) object).getProperty(persistentProperty.name)
                    if (persistentProperty instanceof Embedded) {
                        renderEmbeddedEntity(prop, (Association) persistentProperty, out, "${basePath}${persistentProperty.name}.".toString(), includes, excludes)
                    } else if (persistentProperty instanceof EmbeddedCollection && prop instanceof Iterable) {
                        out.write(JsonToken.OPEN_BRACKET)
                        Iterator iterator = ((Iterable) prop).iterator()
                        while (iterator.hasNext()) {
                            def o = iterator.next()
                            renderEmbeddedEntity(o, (Association) persistentProperty, out, "${basePath}${persistentProperty.name}.".toString(), includes, excludes)
                            if (iterator.hasNext()) {
                                out.write(JsonToken.COMMA)
                            }
                        }
                        out.write(JsonToken.CLOSE_BRACKET)
                    } else {
                        out.write(generator.toJson(((GroovyObject) object).getProperty(persistentProperty.name)))
                    }

                    firstAttribute = false
                }
                out.write(JsonToken.CLOSE_BRACE)
            }

            if (relationships && includeAssociations) {
                out.write(JsonToken.COMMA)
                out.write(generator.toJson('relationships'))
                out.write(JsonToken.COLON)
                out.write(JsonToken.OPEN_BRACE)
                boolean firstRelationship = true

                for (association in relationships) {
                    if (!includeExcludeSupport.shouldInclude(includes, excludes, "${basePath}${association.name}".toString())) {
                        continue
                    }

                    def value = ((GroovyObject) object).getProperty(association.name)
                    if (!firstRelationship) {
                        out.write(JsonToken.COMMA)
                    }
                    firstRelationship = false
                    out.write(generator.toJson(association.name))
                    out.write(JsonToken.COLON)
                    out.write(JsonToken.OPEN_BRACE)

                    if (association instanceof ToOne && value != null) {
                        renderRelationshipLinks(value).writeTo(out)
                        out.write(JsonToken.COMMA)
                    }

                    out.write(generator.toJson('data'))
                    out.write(JsonToken.COLON)
                    PersistentEntity associatedEntity = association.associatedEntity
                    if (association instanceof ToMany && Iterable.isAssignableFrom(association.type)) {
                        out.write(JsonToken.OPEN_BRACKET)
                        if (value != null) {
                            Iterator iterator = ((Iterable) value).iterator()
                            String type = associatedEntity.decapitalizedName

                            while (iterator.hasNext()) {
                                def o = iterator.next()
                                out.write(JsonToken.OPEN_BRACE)
                                writeKeyValue(out, 'type', type)
                                out.write(JsonToken.COMMA)
                                writeKeyValue(out, 'id', idGenerator.render(o, associatedEntity.identity))
                                out.write(JsonToken.CLOSE_BRACE)
                                if (iterator.hasNext()) {
                                    out.write(JsonToken.COMMA)
                                }
                            }
                        }

                        out.write(JsonToken.CLOSE_BRACKET)
                    } else {
                        if (value != null) {
                            out.write(JsonToken.OPEN_BRACE)

                            out.write(generator.toJson('type'))
                            out.write(JsonToken.COLON)
                            out.write(generator.toJson(associatedEntity.decapitalizedName))
                            out.write(JsonToken.COMMA)

                            out.write(generator.toJson('id'))
                            out.write(JsonToken.COLON)
                            out.write(generator.toJson(idGenerator.render(value, associatedEntity.identity)))

                            out.write(JsonToken.CLOSE_BRACE)
                        } else {
                            JsonWritable.NULL_OUTPUT.writeTo(out)
                        }
                    }
                    out.write(JsonToken.CLOSE_BRACE)
                }
                out.write(JsonToken.CLOSE_BRACE)
            }
        }

        if (basePath != '') {
            out.write(JsonToken.COMMA)
            renderRelationshipLinks(object).writeTo(out)
        }
        out.write(JsonToken.CLOSE_BRACE)
    }

    private void renderEmbeddedEntity(Object object, Association property, Writer out, String basePath, List<String> includes, List<String> excludes) {
        PersistentEntity persistentEntity = property.getAssociatedEntity()
        out.write(JsonToken.OPEN_BRACE)
        boolean firstAttribute = true
        for (PersistentProperty prop : persistentEntity.getPersistentProperties()) {
            String qualified = "${basePath}${prop.name}"

            if (!includeExcludeSupport.shouldInclude(includes, excludes, qualified)) {
                continue
            }

            if (!firstAttribute) {
                out.write(JsonToken.COMMA)
            }

            out.write(generator.toJson(prop.name))
            out.write(JsonToken.COLON)
            out.write(generator.toJson(((GroovyObject) object).getProperty(prop.name)))

            firstAttribute = false
        }
        out.write(JsonToken.CLOSE_BRACE)
    }

    private JsonWritable renderData(Object object, Map arguments) {
        JsonGenerator generator = getGenerator()
        new JsonWritable() {

            @Override
            Writer writeTo(Writer out) throws IOException {
                out.write(generator.toJson('data'))
                out.write(JsonToken.COLON)

                if (object instanceof Collection) {
                    out.write(JsonToken.OPEN_BRACKET)
                    boolean first = true
                    for (o in object) {
                        if (!first) {
                            out.write(JsonToken.COMMA)
                        }
                        first = false
                        renderResource(o, out, arguments, '')
                    }
                    out.write(JsonToken.CLOSE_BRACKET)
                } else {
                    renderResource(object, out, arguments, '')
                }
                out
            }

        }
    }

    JsonWritable renderErrors(Object object) {
        JsonGenerator generator = getGenerator()
        JsonWritable writable = new JsonWritable() {

            @Override
            Writer writeTo(Writer out) throws IOException {
                out.write(generator.toJson('errors'))
                out.write(JsonToken.COLON)

                Errors errors = (Errors) object.getAt('errors')

                out.write(JsonToken.OPEN_BRACKET)

                List<ObjectError> allErrors = errors.allErrors
                allErrors.eachWithIndex { ObjectError error, int idx ->
                    this.writeError(out, error)
                    if (idx < allErrors.size() - 1) {
                        out.write(JsonToken.COMMA)
                    }
                }

                out.write(JsonToken.CLOSE_BRACKET)

                return out
            }

            protected writeError(Writer out, ObjectError error) {
                out.write(JsonToken.OPEN_BRACE)
                out.write(generator.toJson('code'))
                out.write(JsonToken.COLON)
                out.write(generator.toJson(error.code))
                out.write(JsonToken.COMMA)

                out.write(generator.toJson('detail'))
                out.write(JsonToken.COLON)
                out.write(generator.toJson(message([error: error])))
                out.write(JsonToken.COMMA)

                out.write(generator.toJson('source'))
                out.write(JsonToken.COLON)
                out.write(JsonToken.OPEN_BRACE)

                out.write(generator.toJson('object'))
                out.write(JsonToken.COLON)
                out.write(generator.toJson(error.getObjectName()))
                out.write(JsonToken.COMMA)

                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error

                    out.write(generator.toJson('field'))
                    out.write(JsonToken.COLON)
                    out.write(generator.toJson(fieldError.getField()))
                    out.write(JsonToken.COMMA)

                    out.write(generator.toJson('rejectedValue'))
                    out.write(JsonToken.COLON)
                    out.write(generator.toJson(fieldError.getRejectedValue()))
                    out.write(JsonToken.COMMA)

                    out.write(generator.toJson('bindingError'))
                    out.write(JsonToken.COLON)
                    out.write(generator.toJson(fieldError.isBindingFailure()))
                }

                out.write(JsonToken.CLOSE_BRACE)//source
                out.write(JsonToken.CLOSE_BRACE)//error
            }

        }
        return writable
    }

    JsonWritable renderRelationshipLinks(Object object) {
        JsonGenerator generator = getGenerator()
        new JsonWritable() {

            @Override
            Writer writeTo(Writer out) throws IOException {
                out.write(generator.toJson('links'))
                out.write(JsonToken.COLON)
                out.write(JsonToken.OPEN_BRACE)
                out.write(generator.toJson('self'))
                out.write(JsonToken.COLON)
                out.write(generator.toJson(view.linkGenerator.link(resource: object, method: HttpMethod.GET)))
                out.write(JsonToken.CLOSE_BRACE)
                out
            }

        }
    }

    JsonWritable renderLinks(Object object, Map arguments) {
        JsonGenerator generator = getGenerator()
        JsonWritable writable = new JsonWritable() {

            @Override
            Writer writeTo(Writer out) throws IOException {
                out.write(generator.toJson('links'))
                out.write(JsonToken.COLON)

                out.write(JsonToken.OPEN_BRACE)
                out.write(generator.toJson('self'))
                out.write(JsonToken.COLON)

                if (object instanceof Collection) {
                    out.write(generator.toJson(view.request.uri))

                    if (arguments.get(PAGINATION) instanceof Map) {
                        Map paginationArgs = (Map) arguments.get(PAGINATION)
                        if (!paginationArgs.containsKey(PAGINATION_TOTAL) || !paginationArgs.containsKey(PAGINATION_RESROUCE)) {
                            throw new IllegalArgumentException('JSON API pagination arguments must contain resource and total')
                        }
                        Integer total = (Integer) paginationArgs.get(PAGINATION_TOTAL)
                        Object resource = paginationArgs.get(PAGINATION_RESROUCE)
                        Parameters params = defaultPaginateParams(paginationArgs)
                        List<Link> links = getPaginationLinks(resource, total, params)
                        for (link in links) {
                            out.write(JsonToken.COMMA)
                            writeKeyValue(out, link.rel, link.href)
                        }
                    }
                } else {
                    out.write(generator.toJson(view.linkGenerator.link(resource: object, method: HttpMethod.GET)))
                }

                out.write(JsonToken.CLOSE_BRACE)
                return out
            }

        }
        return writable
    }

    JsonWritable renderIncluded(Object object, Map arguments) {
        List<String> expandProperties = getExpandProperties((JsonView) view, arguments)
        if (!expandProperties.empty && includeAssociations(arguments)) {
            new JsonWritable() {

                @Override
                Writer writeTo(Writer out) throws IOException {
                    out.write(JsonToken.COMMA)
                    writeKey(out, 'included')
                    out.write(JsonToken.OPEN_BRACKET)
                    boolean first = true

                    for (String prop in expandProperties) {
                        if (!first) {
                            out.write(JsonToken.COMMA)
                        }
                        Object itemToInclude = object.getAt(prop)

                        if (itemToInclude instanceof Collection) {
                            for (o in itemToInclude) {
                                if (!first) {
                                    out.write(JsonToken.COMMA)
                                }
                                first = false
                                renderResource(o, out, arguments, "${prop}.")
                            }
                        } else {
                            renderResource(itemToInclude, out, arguments, "${prop}.")
                        }
                        first = false
                    }
                    out.write(JsonToken.CLOSE_BRACKET)
                    out
                }

            }
        } else {
            return JsonWritable.NOOP_OUTPUT
        }
    }

    void renderMetaObject(Writer out, Object meta) {
        writeKey(out, 'meta')
        viewHelper.render(meta, [:]).writeTo(out)
    }

    void renderJsonApiMember(Writer out, Object meta) {
        writeKey(out, 'jsonapi')
        out.write(JsonToken.OPEN_BRACE)
        writeKeyValue(out, 'version', '1.0')
        if (meta != null) {
            out.write(JsonToken.COMMA)
            renderMetaObject(out, meta)
        }
        out.write(JsonToken.CLOSE_BRACE)
    }

    void renderException(Writer out, Throwable object) {
        JsonGenerator generator = getGenerator()

        StackTraceUtils.sanitize(object)
        out.write(generator.toJson('errors'))
        out.write(JsonToken.COLON)
        out.write(JsonToken.OPEN_BRACKET)
        out.write(JsonToken.OPEN_BRACE)
        writeKeyValue(out, 'status', 500)
        out.write(JsonToken.COMMA)
        writeKeyValue(out, 'title', object.class.name)
        out.write(JsonToken.COMMA)
        writeKeyValue(out, 'detail', object.localizedMessage)
        out.write(JsonToken.COMMA)
        out.write(generator.toJson('source'))
        out.write(JsonToken.COLON)
        out.write(JsonToken.OPEN_BRACE)
        writeKeyValue(out, 'stacktrace', getJsonStackTrace(object))
        out.write(JsonToken.CLOSE_BRACE)//source
        out.write(JsonToken.CLOSE_BRACE)//error
        out.write(JsonToken.CLOSE_BRACKET)
    }

    JsonApiIdRenderStrategy getIdGenerator() {
        ((JsonView) view).jsonApiIdRenderStrategy
    }

}
