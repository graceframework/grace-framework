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
package org.grails.compiler.injection

import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import grails.artefact.Artefact

class EntityASTTransformationSpec extends Specification {

    def "Test Entity class was applied by EntityASTTransformation"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true

        when:
        def clazz = gcl.parseClass('''
@grails.persistence.Entity
class Message {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/Message.groovy')

        def classNode = gcl.getClassNode('Message')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.EntityASTTransformation')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.converters.ConvertersDomainTransformer')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerDomainTransformer')
    }

}
