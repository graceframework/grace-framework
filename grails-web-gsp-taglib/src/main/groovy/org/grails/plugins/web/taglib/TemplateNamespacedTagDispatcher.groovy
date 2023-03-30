/*
 * Copyright 2011-2023 the original author or authors.
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
package org.grails.plugins.web.taglib

import groovy.transform.CompileStatic
import org.springframework.beans.factory.InitializingBean

import org.grails.taglib.NamespacedTagDispatcher
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagLibraryMetaUtils
import org.grails.taglib.TagOutput
import org.grails.taglib.encoder.OutputContextLookupHelper

/**
 * Allows dispatching to namespaced tag libraries and is used within controllers and tag libraries
 * to allow namespaced tags to be invoked as methods (eg. tmpl.person(model: )).
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.0
 */
@CompileStatic
class TemplateNamespacedTagDispatcher extends GroovyObjectSupport implements NamespacedTagDispatcher, InitializingBean {

    public static final String TEMPLATE_NAMESPACE = 'tmpl'
    public static final String RENDER_TAG_NAME = 'render'

    private String namespace = TEMPLATE_NAMESPACE
    private TagLibraryLookup lookup

    TemplateNamespacedTagDispatcher() {
    }

    @Override
    void afterPropertiesSet() throws Exception {
        initializeMetaClass()
    }

    void initializeMetaClass() {
        ExpandoMetaClass emc = new ExpandoMetaClass(getClass(), false, true)
        emc.initialize()
        setMetaClass(emc)
        registerTagMetaMethods(emc)
    }

    void registerTagMetaMethods(ExpandoMetaClass emc) {
        TagLibraryMetaUtils.registerTagMetaMethods(emc, this.lookup, this.namespace)
    }

    @Override
    String getNamespace() {
        this.namespace
    }

    @Override
    void setTagLibraryLookup(TagLibraryLookup lookup) {
        if (!this.lookup) {
            this.lookup = lookup
        }
    }

    @Override
    def methodMissing(String name, Object args) {
        ((GroovyObject) getMetaClass()).setProperty(name, { Object[] varArgs ->
            callRender(argsToAttrs(name, varArgs), filterBodyAttr(varArgs))
        })
        callRender(argsToAttrs(name, args), filterBodyAttr(args))
    }

    private callRender(Map attrs, Object body) {
        TagOutput.captureTagOutput(lookup, TagOutput.DEFAULT_NAMESPACE, RENDER_TAG_NAME, attrs, body,
                OutputContextLookupHelper.lookupOutputContext())
    }

    private Map argsToAttrs(String name, Object args) {
        Map<String, Object> attr = [:]
        attr.template = name
        if (args instanceof Object[]) {
            Object[] tagArgs = ((Object[]) args)
            if (tagArgs.length > 0 && tagArgs[0] instanceof Map) {
                Map<String, Object> modelMap = (Map<String, Object>) tagArgs[0]
                Object encodeAs = modelMap.remove(TagOutput.ENCODE_AS_ATTRIBUTE_NAME)
                if (encodeAs != null) {
                    attr.put(TagOutput.ENCODE_AS_ATTRIBUTE_NAME, encodeAs)
                }
                attr.put("model", modelMap)
            }
        }
        attr
    }

    private Object filterBodyAttr(Object args) {
        if (args instanceof Object[]) {
            Object[] tagArgs = ((Object[]) args)
            if (tagArgs.length > 0) {
                for (Object arg : tagArgs) {
                    if (!(arg instanceof Map)) {
                        return arg
                    }
                }
            }
        }
        return null
    }

}
