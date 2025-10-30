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
package org.grails.views.json.compiler

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

import grails.compiler.traits.TraitInjector
import grails.views.compiler.ViewsTransform

import org.grails.io.support.GrailsFactoriesLoader
import org.grails.views.json.JsonViewWritableScript

/**
 * @author Graeme ROcher
 * @since 2024.0.0
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class JsonViewsTransform extends ViewsTransform {

    JsonViewsTransform(String extension, String dynamicPrefix) {
        super(extension, dynamicPrefix)
    }

    JsonViewsTransform(String extension) {
        super(extension)
    }

    @Override
    protected List<TraitInjector> findTraitInjectors() {
        def injectors = super.findTraitInjectors()

        injectors += GrailsFactoriesLoader.loadFactories(TraitInjector).findAll { TraitInjector ti ->
            ti.artefactTypes.contains(JsonViewWritableScript.TYPE)
        }
        return injectors
    }

}
