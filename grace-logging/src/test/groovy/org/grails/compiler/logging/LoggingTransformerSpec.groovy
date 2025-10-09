/*
 * Copyright 2011-2025 the original author or authors.
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
package org.grails.compiler.logging

import org.slf4j.Logger
import spock.lang.Specification

import grails.compiler.ast.ClassInjector

import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
class LoggingTransformerSpec extends Specification {

    def "Test log field is added to Application classes in 'app/boot'"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def cls = gcl.parseClass('''
class Application {
    def index() {
        return log
    }
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Application.groovy')
        def controller = cls.newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger
    }

    def "Test log field is added to Application classes in 'src/main'"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def cls = gcl.parseClass('''
class Application {
    def index() {
        return log
    }
}
''', '/Users/grails/grails-demo-project/src/main/groovy/org/demo/Application.groovy')
        def controller = cls.newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger
    }

}
