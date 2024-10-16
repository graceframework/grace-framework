/*
 * Copyright 2016-2023 the original author or authors.
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
package org.grails.gsp

import groovy.transform.CompileStatic

import org.grails.taglib.TagLibNamespaceMethodDispatcher
import org.grails.taglib.encoder.OutputContext

/**
 * Base class for staticly compiled GSPs
 *
 * getProperty and invokeMethod calls are a result of GroovyPageTypeCheckingExtension
 *
 */
@CompileStatic
abstract class CompileStaticGroovyPage extends GroovyPage {

    TagLibNamespaceMethodDispatcher defaultTagDispatcher

    @Override
    void initRun(Writer target, OutputContext outputContext, GroovyPageMetaInfo metaInfo) {
        super.initRun(target, outputContext, metaInfo)
        defaultTagDispatcher = TagLibNamespaceMethodDispatcher.cast(lookupTagDispatcher(DEFAULT_NAMESPACE))
    }

    @Override
    protected Object lookupTagDispatcher(String namespace) {
        gspTagLibraryLookup?.hasNamespace(namespace) ? new TagLibNamespaceMethodDispatcher(namespace, gspTagLibraryLookup, outputContext) : null
    }

    @Override
    Object getProperty(String property) {
        resolveProperty(property)
    }

    @Override
    Object invokeMethod(String name, Object args) {
        defaultTagDispatcher.invokeMethod(name, args)
    }

}
