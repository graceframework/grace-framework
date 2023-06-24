/*
 * Copyright 2022-2023 the original author or authors.
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
import org.codehaus.groovy.control.CompilerConfiguration
import spock.lang.Specification

import grails.artefact.Artefact
import grails.artefact.Enhanced

/**
 * @author Michael Yan
 * @since 2022.3.0
 */
class ControllerArtefactTypeTransformationSpec extends Specification {

    def "Test Controller class was applied by ControllerArtefactTypeTransformation"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation',
                                                           'org.grails.compiler.injection.GlobalGrailsPluginTransformation'] as Set<String>)
        def gcl = new TestGroovyClassLoader(getClass().getClassLoader(), configuration)
        gcl.enableInjectTraits = false

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
