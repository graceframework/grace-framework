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
package org.grails.compiler.injection

import java.security.CodeSource

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import org.springframework.boot.autoconfigure.SpringBootApplication
import spock.lang.Specification

import grails.compiler.ast.ClassInjector

/**
 * @author Michael Yan
 * @since 2022.3.0
 */
class ApplicationClassInjectorSpec extends Specification {

    def "Test Application class was injected on '@SpringBootApplication'"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new ApplicationClassInjector()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')

        then:
        clazz.getAnnotationsByType(SpringBootApplication).size() == 1
    }

    def "Test Application class was not injected on '@SpringBootApplication', because annotated with wrong type"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new ApplicationClassInjector()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("App")
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')

        then:
        !clazz.getAnnotationsByType(SpringBootApplication)
    }

    def "Test Application class NOT injected on '@SpringBootApplication'"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new ApplicationClassInjector()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        when:
        def clazz = gcl.parseClass('''
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')

        then:
        !clazz.getAnnotationsByType(SpringBootApplication)
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
            }

        }, Phases.CANONICALIZATION)

        this.compilationUnit = compilationUnit
    }

    ClassNode getClassNode(String name) {
        this.compilationUnit.getClassNode(name)
    }

}
