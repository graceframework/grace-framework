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
package org.grails.compiler.web.taglib

import spock.lang.Issue
import spock.lang.Specification

import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

class TagLibraryTransformerSpec extends Specification {

    @Issue('GRAILS-11241')
    void 'Test that a tag library can be marked with @CompileStatic without generating compile errors'() {
        given:
        def transformer = new TagLibraryTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        expect:
        gcl.parseClass('''
@groovy.transform.CompileStatic
class StaticallyCompiledTagLib implements grails.artefact.TagLibrary {
    def closureTagWithNoExplicitArgs = { }
    def closureTagWithOneArg = { attrs -> }
    def closureTagWithTwoArgs = { attrs, body -> }
}
''', '/Users/grails/grails-demo-project/grails-app/taglib/org/demo/StaticallyCompiledTagLib.groovy')
    }

    void 'Test that a tag library injected method "$getTagLibNamespace", "tagOne"'() {
        given:
        def transformer = new TagLibraryTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        def taglibClass = gcl.parseClass('''
class DemoTagLib {
    static namespace = 'demo'
    def tagOne = { attrs -> }
}
''', '/Users/grails/grails-demo-project/grails-app/taglib/org/demo/DemoTagLib.groovy')

        def taglibMethodNames = taglibClass.getDeclaredMethods()*.name

        expect:
        taglibMethodNames.containsAll('setNamespace', 'getNamespace', '$getTagLibNamespace', 'getTagOne', 'setTagOne', 'tagOne')
    }

}
