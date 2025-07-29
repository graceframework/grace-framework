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

import org.springframework.boot.autoconfigure.SpringBootApplication
import spock.lang.Specification

import grails.compiler.ast.ClassInjector

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class ApplicationClassInjectorSpec extends Specification {

    def "Test Application class was injected on '@SpringBootApplication'"() {
        given:
        def transformer = new ApplicationClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')

        then:
        clazz.getAnnotationsByType(SpringBootApplication).size() == 1
    }

    def "Test Application class was not injected on '@SpringBootApplication', because 'Application.groovy' not in 'grails-app/init'"() {
        given:
        def transformer = new ApplicationClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Application.groovy')

        then:
        !clazz.getAnnotationsByType(SpringBootApplication)
    }

    def "Test Application class annotated on '@SpringBootApplication', and it's in PLUGIN project"() {
        given:
        def transformer = new ApplicationClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'PLUGIN'
        ]

        when:
        def clazz = gcl.parseClass('''
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')

        then:
        clazz.getAnnotationsByType(SpringBootApplication)
    }

}
