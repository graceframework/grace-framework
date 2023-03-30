/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.taglib

import groovy.transform.CompileStatic

import grails.core.GrailsApplication
import grails.util.Environment

/**
 * Allows dispatching to namespaced tag libraries and is used within controllers and tag libraries
 * to allow namespaced tags to be invoked as methods (eg. g.link(action:'foo')).
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.0
 */
@CompileStatic
class DefaultNamespacedTagDispatcher extends GroovyObjectSupport implements NamespacedTagDispatcher {

    protected String namespace
    protected GrailsApplication application
    protected Class type
    protected TagLibraryLookup lookup
    protected boolean developmentMode

    DefaultNamespacedTagDispatcher(String ns, Class callingType, GrailsApplication application, TagLibraryLookup lookup) {
        this.namespace = ns
        this.application = application
        this.developmentMode = Environment.isDevelopmentMode()
        this.lookup = lookup
        this.type = callingType ?: this.getClass()
        initializeMetaClass()
    }

    void initializeMetaClass() {
        // use per-instance metaclass
        ExpandoMetaClass emc = new ExpandoMetaClass(getClass(), false, true)
        emc.initialize()
        setMetaClass(emc)
        registerTagMetaMethods(emc)
    }

    void registerTagMetaMethods(ExpandoMetaClass emc) {
        TagLibraryMetaUtils.registerTagMetaMethods(emc, lookup, namespace)
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
        TagLibraryMetaUtils.methodMissingForTagLib(getMetaClass(), type, lookup, namespace, name, args, !developmentMode)
    }

}
