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
package org.grails.compiler.web

import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import grails.artefact.Artefact
import grails.artefact.Enhanced
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class ControllerDomainTransformerSpec extends Specification {

    def "Test Domain class was injected ControllersDomainBindingApi"() {
        given:
        def transformer = new ControllerDomainTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]

        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'domain', 'org', 'demo', 'Post.groovy'].join(File.separator))

        def classNode = gcl.getClassNode('Post')

        expect: 'injected methods as expect'
        transformer.artefactType == 'Domain'
        classNode.getField('instanceControllersDomainBindingApi')
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getAnnotations(ClassHelper.make(Enhanced))
    }

}
