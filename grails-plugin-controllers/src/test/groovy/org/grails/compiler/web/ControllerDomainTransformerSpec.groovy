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

import java.security.CodeSource

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Michael Yan
 * @since 2022.3.0
 */
class ControllerDomainTransformerSpec extends Specification {

    def "Test Domain class was injected ControllersDomainBindingApi"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new ControllerDomainTransformer()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''', "grails-demo-project/grails-app/domain/org/demo/Post.groovy")

        def classNode = gcl.getClassNode('Post')

        and:
        expect: 'injected methods as expect'
        transformer.artefactType == 'Domain'
        classNode.getField('instanceControllersDomainBindingApi') != null
    }

}


class TestGrailsAwareClassLoader extends GrailsAwareClassLoader {
    CompilationUnit compilationUnit

    TestGrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration configuration, ClassInjector[] classInjectors) {
        super(parent, configuration)
        setClassInjectors(classInjectors)
    }

    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit compilationUnit = super.createCompilationUnit(config, source)
        compilationUnit.addFirstPhaseOperation(new CompilationUnit.IPrimaryClassNodeOperation() {

            @Override
            void call(SourceUnit sourceUnit, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                sourceUnit.getAST().putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
                sourceUnit.getAST().putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
                sourceUnit.getAST().putNodeMetaData('PROJECT_TYPE', 'WEB_APP')
            }

        }, Phases.CANONICALIZATION)

        this.compilationUnit = compilationUnit
    }

    ClassNode getClassNode(String name) {
        this.compilationUnit.getClassNode(name)
    }

}
