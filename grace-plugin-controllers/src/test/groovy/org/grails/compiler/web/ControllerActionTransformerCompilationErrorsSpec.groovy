/*
 * Copyright 2011-2026 the original author or authors.
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
package org.grails.compiler.web

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spock.lang.Specification

import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

class ControllerActionTransformerCompilationErrorsSpec extends Specification {

    static gcl

    void setupSpec() {
        def transformer = new ControllerActionTransformer()
        gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]
    }

    void 'Test overloaded method actions'() {
        when: 'A controller overloads a method action'
        gcl.parseClass('''
class TestController {
    def methodAction(String s) {
    }

    def methodAction(Integer i) {
    }
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'demo', 'TestController.groovy'].join(File.separator))
        then:
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Controller actions may not be overloaded. The [methodAction] action has been overloaded in [TestController].'
    }

    void "Test default parameter values"() {
        when: 'A method action has default parameter values'
        gcl.parseClass('''
class TestController {
    def methodAction(int i = 42) {
    }
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'demo', 'TestController.groovy'].join(File.separator))

        then:
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Parameter [i] to method [methodAction] has default value [42]. Default parameter values are not allowed in controller action methods.'
    }
}
