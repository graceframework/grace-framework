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
package grails.gsp.taglib.compiler

import java.security.CodeSource

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Michael Yan
 * @since 2022.3.0
 */
class TagLibArtefactTypeAstTransformationSpec extends Specification {

    def "Test TagLib class was applied by TagLibArtefactTypeAstTransformation"() {
        given:
        CompilerConfiguration configuration = new CompilerConfiguration()
        def gcl = new TestGrailsAwareClassLoader(getClass().getClassLoader(), configuration)

        when:
        def clazz = gcl.parseClass('''
@grails.gsp.TagLib
class FooTagLib {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/FooTagLib.groovy')

        def classNode = gcl.getClassNode('FooTagLib')

        then:
        clazz.getAnnotationsByType(Artefact).size() == 1
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_grails.gsp.taglib.compiler.TagLibArtefactTypeAstTransformation')
    }

}


class TestGrailsAwareClassLoader extends GrailsAwareClassLoader {
    CompilationUnit compilationUnit

    TestGrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration configuration, ClassInjector[] classInjectors = []) {
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
