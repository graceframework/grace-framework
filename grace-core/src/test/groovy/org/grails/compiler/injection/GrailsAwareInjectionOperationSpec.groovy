/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.compiler.injection

import spock.lang.Specification

import grails.compiler.ast.ClassInjector

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class GrailsAwareInjectionOperationSpec extends Specification {

    def "Test Grails have 7 class injectors"() {
        given:
        def classInjectors = [
                'org.grails.compiler.injection.ApplicationClassInjector',
                'org.grails.compiler.boot.BootInitializerClassInjector',
                'org.grails.compiler.web.converters.ConvertersDomainTransformer',
                'org.grails.compiler.web.ControllerDomainTransformer',
                'org.grails.compiler.web.ControllerActionTransformer',
                'org.grails.compiler.web.taglib.TagLibraryTransformer',
                'org.grails.gsp.compiler.transform.GroovyPageBytecodeOptimizer',
                'org.grails.compiler.injection.DefaultGrailsDomainClassInjector',
                'org.grails.compiler.gorm.GormTransformer'
        ]

        expect:
        GrailsAwareInjectionOperation.classInjectors.length == 9
        GrailsAwareInjectionOperation.classInjectors*.class.name.containsAll(classInjectors)
    }

    def "Test Grails set local class injectors"() {
        given:
        def injector = new ApplicationClassInjector()
        def injectionOperation = new GrailsAwareInjectionOperation([injector] as ClassInjector[])
        def classInjectors = [
                'org.grails.compiler.injection.ApplicationClassInjector'
        ]

        expect:
        injectionOperation.localClassInjectors.length == 1
        injectionOperation.localClassInjectors*.class.name.containsAll(classInjectors)
    }

}
