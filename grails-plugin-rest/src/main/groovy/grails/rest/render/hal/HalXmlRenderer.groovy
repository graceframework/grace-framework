/*
 * Copyright 2013-2022 the original author or authors.
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
package grails.rest.render.hal

import java.beans.PropertyDescriptor

import groovy.transform.CompileStatic
import org.springframework.beans.BeanWrapper
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.http.HttpMethod

import grails.converters.XML
import grails.rest.Link
import grails.rest.render.RenderContext
import grails.rest.render.util.AbstractLinkingRenderer
import grails.web.mime.MimeType

import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.web.xml.PrettyPrintXMLStreamWriter
import org.grails.web.xml.StreamingMarkupWriter
import org.grails.web.xml.XMLStreamWriter

/**
 * Renders domain instances in HAL XML format (see http://stateless.co/hal_specification.html)
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class HalXmlRenderer<T> extends AbstractLinkingRenderer<T> {

    public static final MimeType MIME_TYPE = MimeType.HAL_XML
    public static final String RESOURCE_TAG = 'resource'
    public static final String LINK_TAG = 'link'
    public static final String RELATIONSHIP_ATTRIBUTE = 'rel'

    private static final MimeType[] DEFAULT_MIME_TYPES = [MIME_TYPE] as MimeType[]

    HalXmlRenderer(Class<T> targetType) {
        super(targetType, DEFAULT_MIME_TYPES)
    }

    HalXmlRenderer(Class<T> targetType, MimeType mimeType) {
        super(targetType, mimeType)
    }

    HalXmlRenderer(Class<T> targetType, MimeType[] mimeTypes) {
        super(targetType, mimeTypes)
    }

    @Override
    void renderInternal(T object, RenderContext context) {
        StreamingMarkupWriter streamingWriter = new StreamingMarkupWriter(context.writer, encoding)
        XMLStreamWriter w = prettyPrint ? new PrettyPrintXMLStreamWriter(streamingWriter) : new XMLStreamWriter(streamingWriter)
        XML xml = new XML(w)

        PersistentEntity entity = mappingContext.getPersistentEntity(object.class.name)
        boolean isDomain = entity != null

        Set writtenObjects = []
        w.startDocument(encoding, '1.0')

        if (isDomain) {
            writeDomainWithEmbeddedAndLinks(entity, object, context, xml, writtenObjects)
        }
        else if (object instanceof Collection) {
            XMLStreamWriter writer = xml.getWriter()
            startResourceTagForCurrentPath(context, writer)
            for (o in ((Collection) object)) {
                PersistentEntity currentEntity = mappingContext.getPersistentEntity(o.class.name)
                if (currentEntity) {
                    writeDomainWithEmbeddedAndLinks(currentEntity, o, context, xml, writtenObjects)
                }
            }
            writer.end()
        }
        else {
            XMLStreamWriter writer = xml.getWriter()
            startResourceTagForCurrentPath(context, writer)
            writeExtraLinks(object, context.locale, xml)
            BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(object)
            PropertyDescriptor[] propertyDescriptors = bean.propertyDescriptors
            for (pd in propertyDescriptors) {
                String propertyName = pd.name
                if (DEFAULT_EXCLUDES.contains(propertyName)) {
                    continue
                }
                if (shouldIncludeProperty(context, object, propertyName)) {
                    if (pd.readMethod && pd.writeMethod) {
                        writer.startNode(propertyName)
                        xml.convertAnother(bean.getPropertyValue(propertyName))
                        writer.end()
                    }
                }
            }
            writer.end()
        }
    }

    protected void startResourceTagForCurrentPath(RenderContext context, XMLStreamWriter writer) {
        Locale locale = context.locale
        String resourceHref = linkGenerator.link(uri: context.resourcePath, method: HttpMethod.GET, absolute: absoluteLinks)
        String title = getResourceTitle(context.resourcePath, locale)
        startResourceTag(writer, resourceHref, locale, title)
    }

    protected void writeDomainWithEmbeddedAndLinks(PersistentEntity entity, object, RenderContext context, XML xml, Set writtenObjects) {
        Locale locale = context.locale
        String resourceHref = linkGenerator.link(resource: object, method: HttpMethod.GET, absolute: absoluteLinks)
        String title = getLinkTitle(entity, locale)
        XMLStreamWriter writer = xml.getWriter()
        startResourceTag(writer, resourceHref, locale, title)
        MetaClass metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        Map<Association, Object> associationMap = writeAssociationLinks(context, object, locale, xml, entity, metaClass)
        writeDomain(context, metaClass, entity, object, xml)

        if (associationMap) {
            for (entry in associationMap.entrySet()) {
                Association property = entry.key
                boolean isSingleEnded = property instanceof ToOne
                if (isSingleEnded) {
                    Object value = entry.value
                    if (writtenObjects.contains(value)) {
                        continue
                    }

                    if (value != null) {
                        PersistentEntity associatedEntity = property.associatedEntity
                        if (associatedEntity) {
                            writtenObjects << value
                            writeDomainWithEmbeddedAndLinks(associatedEntity, value, context, xml, writtenObjects)
                        }
                    }
                }
                else {
                    PersistentEntity associatedEntity = property.associatedEntity
                    if (associatedEntity) {
                        for (obj in entry.value) {
                            writtenObjects << obj
                            writeDomainWithEmbeddedAndLinks(associatedEntity, obj, context, xml, writtenObjects)
                        }
                    }
                }
            }
        }
        writer.end()
    }

    protected void startResourceTag(XMLStreamWriter writer, String resourceHref, Locale locale, String title) {
        writer.startNode(RESOURCE_TAG)
                .attribute(HREF_ATTRIBUTE, resourceHref)
                .attribute(HREFLANG_ATTRIBUTE, locale.language)

        if (title) {
            writer.attribute(TITLE_ATTRIBUTE, title)
        }
    }

    @Override
    void writeLink(Link link, Locale locale, writerObject) {
        XMLStreamWriter writer = ((XML) writerObject).getWriter()
        writer.startNode(LINK_TAG)
                .attribute(RELATIONSHIP_ATTRIBUTE, link.rel)
                .attribute(HREF_ATTRIBUTE, link.href)
                .attribute(HREFLANG_ATTRIBUTE, (link.hreflang ?: locale).language)

        String title = link.title
        if (title) {
            writer.attribute(TITLE_ATTRIBUTE, title)
        }
        String contentType = link.contentType
        if (contentType) {
            writer.attribute(TYPE_ATTRIBUTE, contentType)
        }

        if (link.templated) {
            writer.attribute(TEMPLATED_ATTRIBUTE, 'true')
        }
        if (link.deprecated) {
            writer.attribute(DEPRECATED_ATTRIBUTE, 'true')
        }
        writer.end()
    }

    @Override
    protected void writeDomainProperty(value, String propertyName, writerObject) {
        XML xml = (XML) writerObject
        XMLStreamWriter writer = xml.getWriter()

        writer.startNode(propertyName)
        xml.convertAnother(value)
        writer.end()
    }

}
