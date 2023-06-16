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
package org.grails.compiler.web.converters

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.compiler.ast.ClassInjector
import grails.converters.XML
import grails.persistence.Entity
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2022.3.0
 */
class ConvertersDomainTransformerSpec extends Specification {

    def "Test Domain class was injected asType"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def classInjector = new ConvertersDomainTransformer()
        gcl.classInjectors = [classInjector] as ClassInjector[]

        def domainClass = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''', "grails-demo-project/grails-app/domain/org/demo/Post.groovy")

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy'

        ClassNode classNode = new ClassNode(domainClass)
        classNode.setModule(moduleNode)

        def domainMethodNames = domainClass.getMethods()*.name

        and:
        List<String> injectedMethodNames = [
                "asType"
        ]

        expect: 'injected methods as expect'
        classInjector.artefactType == 'Domain'
        classInjector.shouldInject(classNode)
        classNode.getField('instanceConvertersApi') != null

        injectedMethodNames.each { methodName ->
            assert methodName in domainMethodNames
        }
    }

    void "Test domain type conversion methods added at compile time"() {
        when:
        def xml = new ConvertMe(name: "Bob") as XML

        then:
        xml instanceof XML
    }

}

@Entity
class ConvertMe {
    String name
}

