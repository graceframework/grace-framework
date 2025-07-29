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
package org.grails.compiler.web

import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import grails.artefact.Artefact
import grails.artefact.Enhanced
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class ControllerArtefactTypeTransformationSpec extends Specification {

    def "Test Controller class was applied by ControllerArtefactTypeTransformation"() {
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
@grails.web.Controller
class FooController {
  def index() {}
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/FooController.groovy')

        def classNode = gcl.getClassNode('FooController')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getAnnotations(ClassHelper.make(Enhanced))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerArtefactTypeTransformation')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerActionTransformer')
    }

}
