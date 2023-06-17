/*
 * Copyright 2011-2023 the original author or authors.
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
package org.grails.compiler.web.taglib

import java.security.CodeSource

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Issue
import spock.lang.Specification

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

class TagLibraryTransformerSpec extends Specification {

    void 'Test tag methods are created for properties which are tags'() {
        expect:
        /*
         * Tag methods are overloaded with these argument combinations:
         *    tagName()
         *    tagName(Map)
         *    tagName(Closure)
         *    tagName(Map, Closure)
         *    tagName(Map, CharSequence)
         */
        5 == ClosureMethodTestTagLib.methods.findAll { methodName == it.name }.size()

        where:
        methodName << ['closureTagWithNoExplicitArgs', 'closureTagWithOneArg', 'closureTagWithTwoArgs']
    }

    void 'Test tag methods are not created for properties which are not tags'() {
        expect:
        0 == ClosureMethodTestTagLib.methods.findAll { methodName == it.name }.size()

        where:
        methodName << ['closureTagWithThreeArgs', 'closureTagWithFourArgs']
    }

    @Issue('GRAILS-11241')
    void 'Test that a tag library can be marked with @CompileStatic without generating compile errors'() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def transformer = new TagLibraryTransformer() {
            @Override
            boolean shouldInject(ClassNode classNode) { true }
        }
        gcl.classInjectors = [transformer] as ClassInjector[]

        expect:
        gcl.parseClass('''
        @groovy.transform.CompileStatic
        class StaticallyCompiledTagLib implements grails.artefact.TagLibrary {
            def closureTagWithNoExplicitArgs = { }
            def closureTagWithOneArg = { attrs -> }
            def closureTagWithTwoArgs = { attrs, body -> }
        }
        ''')
    }

    void 'Test that a tag library injected method "$getTagLibNamespace", "tagOne"'() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        def transformer = new TagLibraryTransformer()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration, [transformer] as ClassInjector[])

        def taglibClass = gcl.parseClass('''
class DemoTagLib {
    static namespace = 'demo'
    def tagOne = { attrs -> }
}
''', '/Users/grails/grails-demo-project/grails-app/taglib/org/demo/DemoTagLib.groovy')

        def taglibMethodNames = taglibClass.getDeclaredMethods()*.name
        def classNode = gcl.getClassNode('DemoTagLib')

        expect:
        classNode
        classNode.getModule().getNodeMetaData('PROJECT_DIR')
        classNode.getModule().getNodeMetaData('GRAILS_APP_DIR')
        classNode.getAnnotations(new ClassNode(Artefact)).isEmpty()
        transformer.shouldInject(classNode)
        taglibMethodNames.containsAll('setNamespace', 'getNamespace', '$getTagLibNamespace', 'getTagOne', 'setTagOne', 'tagOne')
    }

}

@Artefact('TagLib')
class ClosureMethodTestTagLib {
    def closureTagWithNoExplicitArgs = {}
    def closureTagWithOneArg = { attrs -> }
    def closureTagWithTwoArgs = { attrs, body -> }
    def closureTagWithThreeArgs = { attrs, body, extra -> }
    def closureTagWithFourArgs = { attrs, body, extra, anotherExtra -> }
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
