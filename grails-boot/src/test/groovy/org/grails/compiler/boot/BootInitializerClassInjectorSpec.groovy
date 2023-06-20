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
package org.grails.compiler.boot

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
import grails.util.Environment
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
class BootInitializerClassInjectorSpec extends Specification {

    void "test compile application class"() {
        when: "An application class is compiled"
        def gcl = new GrailsAwareClassLoader()
        Class applicationClass = gcl.parseClass('''
import grails.boot.Grails

@grails.artefact.Artefact("Application")
class Application {
    static void main(String[] args) {
        println "foo"
    }
}
''')

        applicationClass.main()

        then: ""
        Boolean.getBoolean(Environment.STANDALONE)
        Environment.isStandalone()
        !Environment.isStandaloneDeployed()
    }

    def "Test ApplicationLoader class was generated"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new BootInitializerClassInjector()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        applicationLoader
    }

    def "Test ApplicationLoader class was NOT generated in Plugin"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new BootInitializerClassInjector()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        when:
        def clazz = gcl.parseClass('''
@grails.plugins.metadata.PluginSource
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        !applicationLoader
    }

    def "Test ApplicationLoader class was NOT generated in Plugin too"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new BootInitializerClassInjector()
        def gcl = new PluginTestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        !applicationLoader
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

class PluginTestGrailsAwareClassLoader extends GrailsAwareClassLoader {
    CompilationUnit compilationUnit

    PluginTestGrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration configuration, ClassInjector[] classInjectors) {
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
                sourceUnit.getAST().putNodeMetaData('PROJECT_TYPE', 'PLUGIN')
            }

        }, Phases.CANONICALIZATION)

        this.compilationUnit = compilationUnit
    }

    ClassNode getClassNode(String name) {
        this.compilationUnit.getClassNode(name)
    }

}
