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

import groovy.transform.Generated
import org.codehaus.groovy.ast.Parameter
import spock.lang.Specification

/**
 * Test for {@link org.grails.compiler.injection.EnhancesTraitTransformation)
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
class EnhancesTraitTransformationSpec extends Specification {

    def "Test Enhances trait class was generated with TraitInjector"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Enhances("Controller")
trait FooTrait {
    def someMethodInTheFooTrait() {
        "bar"
    }
}
''', '/Users/grails/grails-demo-project/src/main/groovy/org/demo/Foo.groovy')

        then:
        gcl.getClassNode('FooTraitTraitInjector')
    }

    def "Test Enhances trait class was transformed"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Enhances("Controller")
trait Foo {
    def someMethodInTheFooTrait() {
        "bar"
    }
}
''', '/Users/grails/grails-demo-project/src/main/groovy/org/demo/Foo.groovy')

        def classNode = gcl.getClassNode('FooTraitInjector')
        def injectedMethods = ['getTrait', 'getArtefactTypes']

        then:
        classNode.methods*.name.containsAll(injectedMethods)
        injectedMethods.each {
            assert GrailsASTUtils.hasAnnotation(classNode.getMethod(it, [] as Parameter[]), Generated)
        }
    }

}
