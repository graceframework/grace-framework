/*
 * Copyright 2022-2026 the original author or authors.
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
package grails.gsp.taglib.compiler

import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import grails.artefact.Artefact
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class TagLibArtefactTypeAstTransformationSpec extends Specification {

    def "Test TagLib class was applied by TagLibArtefactTypeAstTransformation"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@grails.gsp.TagLib
class FooTagLib {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/FooTagLib.groovy')

        def classNode = gcl.getClassNode('FooTagLib')

        then:
        clazz.getAnnotationsByType(Artefact).size() == 1
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_grails.gsp.taglib.compiler.TagLibArtefactTypeAstTransformation')
    }

    def "Test tag methods are created for properties which are tags"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact('TagLib')
class ClosureMethodTestTagLib {
    def closureTagWithNoExplicitArgs = {}
    def closureTagWithOneArg = { attrs -> }
    def closureTagWithTwoArgs = { attrs, body -> }
    def closureTagWithThreeArgs = { attrs, body, extra -> }
    def closureTagWithFourArgs = { attrs, body, extra, anotherExtra -> }
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/ClosureMethodTestTagLib.groovy')

        def classNode = gcl.getClassNode('ClosureMethodTestTagLib')

        then:
        /*
         * Tag methods are overloaded with these argument combinations:
         *    tagName()
         *    tagName(Map)
         *    tagName(Closure)
         *    tagName(Map, Closure)
         *    tagName(Map, CharSequence)
         */
        5 == classNode.methods.findAll { methodName == it.name }.size()

        where:
        methodName << ['closureTagWithNoExplicitArgs', 'closureTagWithOneArg', 'closureTagWithTwoArgs']
    }

    def "Test tag methods are not created for properties which are not tags"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact('TagLib')
class ClosureMethodTestTagLib {
    def closureTagWithNoExplicitArgs = {}
    def closureTagWithOneArg = { attrs -> }
    def closureTagWithTwoArgs = { attrs, body -> }
    def closureTagWithThreeArgs = { attrs, body, extra -> }
    def closureTagWithFourArgs = { attrs, body, extra, anotherExtra -> }
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/ClosureMethodTestTagLib.groovy')

        def classNode = gcl.getClassNode('ClosureMethodTestTagLib')

        then:
        0 == classNode.methods.findAll { methodName == it.name }.size()

        where:
        methodName << ['closureTagWithThreeArgs', 'closureTagWithFourArgs']
    }

}
