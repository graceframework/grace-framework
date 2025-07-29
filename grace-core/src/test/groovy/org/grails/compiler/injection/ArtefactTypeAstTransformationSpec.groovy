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

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import spock.lang.Issue
import spock.lang.Specification

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.compiler.traits.TraitInjector
import org.grails.core.artefact.ControllerArtefactHandler

/**
 * @author James Kleeh
 * @author Michael Yan
 */
class ArtefactTypeAstTransformationSpec extends Specification {

    void "test resolveArtefactType with string literal"() {
        given:
        ArtefactTypeAstTransformation ast = new ArtefactTypeAstTransformation()
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value", new ConstantExpression("ABC"))

        when:
        String returnValue = ast.resolveArtefactType(null, annotationNode, classNode)

        then:
        returnValue == "ABC"
    }

    void "test resolveArtefactType with property expression"() {
        given:
        ArtefactTypeAstTransformation ast = new ArtefactTypeAstTransformation()
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value",
                new PropertyExpression(
                        new ClassExpression(ClassHelper.make(ControllerArtefactHandler)), "TYPE"))

        when:
        String returnValue = ast.resolveArtefactType(null, annotationNode, classNode)

        then:
        returnValue == "Controller"
    }

    void "test resolveArtefactType with null"() {
        given:
        ArtefactTypeAstTransformation ast = new ArtefactTypeAstTransformation()
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value", null)

        when:
        ast.resolveArtefactType(null, annotationNode, classNode)

        then:
        thrown(RuntimeException)
    }

    @Issue("https://github.com/grails/grails-core/issues/10531")
    void "TraitInjector without SupportsClassNode gets applied to artefacts"() {
        setup:
        TraitInjectionUtils.@traitInjectors = [new TestTraitInjector()]
        GrailsAwareClassLoader gcl = new GrailsAwareClassLoader()

        Class clazz = gcl.parseClass """
			 	@grails.artefact.Artefact("Controller")
				class FooController {
			
				}
			"""

        when:
        def t = clazz.newInstance()

        then:
        t instanceof Test10531Trait
        t.hello10531() == "Hello"

        cleanup:
        TraitInjectionUtils.@traitInjectors = null
    }

    @Issue("https://github.com/grails/grails-core/issues/10531")
    void "TraitInjector with SupportsClassNode gets applied only if supports return true"() {
        setup:
        TraitInjectionUtils.@traitInjectors = [new TestTraitInjectorForSupportsClassNode(false)]
        GrailsAwareClassLoader gcl = new GrailsAwareClassLoader()

        Class clazz = gcl.parseClass """
			 	@grails.artefact.Artefact("Controller")
				class FooController {
			
				}
			"""

        when: "Supports returns false"
        def t = clazz.newInstance()

        then: "Trait is not applied"
        !(t instanceof Test10531Trait)

        when:
        t.hello10531()

        then:
        thrown(MissingMethodException)


        when: "Supports returns true"
        TraitInjectionUtils.@traitInjectors = [new TestTraitInjectorForSupportsClassNode(true)]
        clazz = gcl.parseClass """
			 	@grails.artefact.Artefact("Controller")
				class BarController {
			
				}
			"""

        t = clazz.newInstance()

        then: "Trait is applied"
        t instanceof Test10531Trait
        t.hello10531() == "Hello"


        cleanup:
        TraitInjectionUtils.@traitInjectors = null

    }

    void "Application artefact should be injected by 2 class injectors"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class Application {
}
''')

        def classNode = gcl.getClassNode('Application')

        then:
        ClassInjector[] applicationClassInjectors = classInjectors.findAll { it.shouldInject(classNode) }
        def expectInjectors = [
                'org.grails.compiler.injection.ApplicationClassInjector',
                'org.grails.compiler.boot.BootInitializerClassInjector'
        ]

        expect:
        applicationClassInjectors.length == 2
        applicationClassInjectors*.class.name.containsAll(expectInjectors)
    }

    void "Bootstrap artefact should be injected by 0 class injectors"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Bootstrap")
class BootStrap {
}
''')

        def classNode = gcl.getClassNode('BootStrap')

        then:
        ClassInjector[] bootstrapClassInjectors = classInjectors.findAll { it.shouldInject(classNode) }

        expect:
        !bootstrapClassInjectors
    }

    void "Controller artefact should be injected by 1 class injectors"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class PostController {
}
''')

        def classNode = gcl.getClassNode('PostController')

        then:
        ClassInjector[] controllerClassInjectors = classInjectors.findAll { it.shouldInject(classNode) }
        def expectInjectors = [
                'org.grails.compiler.web.ControllerActionTransformer'
        ]

        expect:
        controllerClassInjectors.length == 1
        controllerClassInjectors*.class.name.containsAll(expectInjectors)
    }

    void "Domain artefact should be injected by 3 class injectors"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''')

        def classNode = gcl.getClassNode('Post')

        then:
        ClassInjector[] domainClassInjectors = classInjectors.findAll { it.shouldInject(classNode) }
        def expectInjectors = [
                'org.grails.compiler.injection.DefaultGrailsDomainClassInjector',
                'org.grails.compiler.web.converters.ConvertersDomainTransformer',
                'org.grails.compiler.web.ControllerDomainTransformer',
                'org.grails.compiler.gorm.GormTransformer'
        ]

        expect:
        domainClassInjectors.length == 4
        domainClassInjectors*.class.name.containsAll(expectInjectors)
    }

    void "TagLib artefact should be injected by 1 class injectors"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
class PostTagLib {
}
''')

        def classNode = gcl.getClassNode('PostTagLib')

        then:
        ClassInjector[] taglibClassInjectors = classInjectors.findAll { it.shouldInject(classNode) }
        def expectInjectors = [
                'org.grails.compiler.web.taglib.TagLibraryTransformer'
        ]

        expect:
        taglibClassInjectors.length == 1
        taglibClassInjectors*.class.name.containsAll(expectInjectors)
    }


    @Artefact("Controller")
    class Test {
    }

    @Artefact(ControllerArtefactHandler.TYPE)
    class Test2 {

    }

    class TestTraitInjector implements TraitInjector {

        @Override
        Class<?> getTrait() {
            Test10531Trait
        }

        @Override
        String[] getArtefactTypes() {
            ["Controller"]
        }
    }

    class TestTraitInjectorForSupportsClassNode implements TraitInjector {
        boolean shouldSupport

        TestTraitInjectorForSupportsClassNode(boolean support) {
            this.shouldSupport = support
        }

        @Override
        Class<?> getTrait() {
            Test10531Trait
        }

        @Override
        String[] getArtefactTypes() {
            ["Controller"]
        }

        @Override
        boolean supports(ClassNode classNode) {
            return shouldSupport
        }
    }

    trait Test10531Trait {
        def hello10531() { return "Hello" }
    }

}
