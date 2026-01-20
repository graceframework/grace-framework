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
package org.grails.compiler.gorm

import spock.lang.Specification

import org.codehaus.groovy.ast.ClassHelper

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * Test {@link GormTransformer}
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
class GormTransformerSpec extends Specification {

    def "Test Domain class was applied by 'GormTransformer'"() {
        given:
        def transformer = new GormTransformer()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grace', 'grace-demo-project', 'app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grace', 'grace-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Person {
}
''', ['', 'Users', 'grace', 'grace-demo-project', 'app', 'domain', 'org', 'demo', 'Person.groovy'].join(File.separator))

        def classNode = gcl.getClassNode('Person')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.gorm.GormTransformer')
    }

}
