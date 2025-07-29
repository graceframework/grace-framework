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
package org.grails.compiler.web.converters

import java.security.CodeSource

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.converters.XML
import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2024.0.0
 */
class ConvertersDomainTransformerSpec extends Specification {

    def "Test Domain class was injected asType"() {
        given:
        def gcl = new TestGroovyClassLoader()

        def domainClass = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''')

        def classNode = gcl.getClassNode('Post')
        def domainMethodNames = domainClass.getMethods()*.name

        and:
        List<String> injectedMethodNames = [
                "asType"
        ]

        expect: 'injected methods as expect'
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


class TestGroovyClassLoader extends GroovyClassLoader {
    CompilationUnit compilationUnit

    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit compilationUnit = super.createCompilationUnit(config, source)
        compilationUnit.addFirstPhaseOperation(new CompilationUnit.IPrimaryClassNodeOperation() {

            @Override
            void call(SourceUnit sourceUnit, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                sourceUnit.getAST().putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
                sourceUnit.getAST().putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
            }

        }, Phases.CANONICALIZATION)
        this.compilationUnit = compilationUnit
    }

    ClassNode getClassNode(String name) {
        this.compilationUnit.getClassNode(name)
    }

}