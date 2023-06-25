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
package org.grails.compiler.web

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.security.CodeSource

import groovy.transform.Generated
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.core.ArtefactHandler
import grails.util.BuildSettings
import grails.util.GrailsWebMockUtil
import grails.web.Action
import grails.web.servlet.context.GrailsWebApplicationContext
import org.grails.compiler.injection.ArtefactTypeAstTransformation
import org.grails.compiler.injection.TraitInjectionUtils
import org.grails.core.io.support.GrailsFactoriesLoader

/**
 * @author Stephane Maldini
 * @author Michael Yan
 * @since 2.0
 */
class ControllerActionTransformerSpec extends Specification {

    def setup() {
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'true'
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        def appCtx = new GrailsWebApplicationContext()
        def servletContext = webRequest.servletContext
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'false'
    }

    def createClassLoader() {
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation',
                                                           'org.grails.compiler.injection.GlobalGrailsPluginTransformation'] as Set<String>)
        def transformer = new ControllerActionTransformer() {
            @Override
            boolean shouldInject(ClassNode classNode) {
                true
            }
        }
        transformer.setCompilationUnit(new CompilationUnit())
        def gcl = new TestGroovyClassLoader(getClass().getClassLoader(), configuration)
        gcl.classInjectors = [transformer] as ClassInjector[]
        return gcl
    }

    void "Test that a closure action has changed to method"() {
        when:
        def gcl = createClassLoader()
        def cls = gcl.parseClass('''
class TestTransformedToController {

    def action = {
    }

}
''')

        def classNode = gcl.getClassNode('TestTransformedToController')
        def controller = cls.newInstance()

        then:
        controller
        controller.getClass().getMethod("action", [] as Class[]) != null
        controller.getClass().getMethod('action', [] as Class[]).getAnnotation(Action)

        and: 'its not marked as Generated'
        !controller.getClass().getMethod("action", [] as Class[]).isAnnotationPresent(Generated)
    }

    void 'Test that user applied annotations are applied to generated action methods'() {
        given:
        def gcl = createClassLoader()
        def cls = gcl.parseClass('''
class SomeController {
    @Deprecated
    def action1(){}
    @Deprecated
    def action2(String paramName){}
}
''')

        when:
        def action1NoArgMethod = cls.getMethod('action1')

        then:
        action1NoArgMethod.getAnnotation(Action)
        action1NoArgMethod.getAnnotation(Deprecated)

        when:
        def action2MethodWithStringArg = cls.getMethod('action2', [String] as Class[])

        then:
        !action2MethodWithStringArg.getAnnotation(Action)
        action2MethodWithStringArg.getAnnotation(Deprecated)

        when:
        def action2NoArgMethod = cls.getMethod('action2')

        then:
        action2NoArgMethod.getAnnotation(Action)
        action2NoArgMethod.getAnnotation(Deprecated)
    }

    void 'Test that a controller may have an abstract method - GRAILS-10509'() {
        given:
        def gcl = createClassLoader()
        def controllerClass = gcl.parseClass('''
abstract class SomeController {
    def someAction() {}
    abstract someAbstractMethod()
}
''')

        when:
        def method = controllerClass.getMethod('someAbstractMethod')

        then:
        Modifier.isAbstract(method.modifiers)

        when:
        method = controllerClass.getMethod('someAction')

        then:
        !Modifier.isAbstract(method.modifiers)
    }

    void 'Test action overriding'() {
        given:
        def gcl = createClassLoader()
        def superControllerClass = gcl.parseClass('''
class SuperController {
    def methodAction() {
        [ actionInvoked: 'SuperController.methodAction' ]
    }
    def methodActionWithParam(String s) {
        [ paramValue: s ]
    }
}
class SubController extends SuperController {
    def methodAction() {
        [ actionInvoked: 'SubController.methodAction' ]
    }
    def methodActionWithParam(Integer i) {
        [ paramValue: i ]
    }
}
''', "grails-demo-project/grails-app/controllers/org/demo/SuperController.groovy")

        def subControllerClass = gcl.loadClass('SubController')
        def superController = superControllerClass.newInstance()
        def subController = subControllerClass.newInstance()

        when:
        def model = superController.methodAction()

        then:
        'SuperController.methodAction' == model.actionInvoked

        when:
        superController.params.s = 'Super Controller Param'
        model = superController.methodActionWithParam()

        then:
        'Super Controller Param' == model.paramValue

        when:
        model = subController.methodAction()

        then:
        'SubController.methodAction' == model.actionInvoked

        when:
        subController.params.s = 'Super Controller Param'
        model = subController.methodActionWithParam()

        then:
        null == model.paramValue

        when:
        subController.params.i = 42
        model = subController.methodActionWithParam()

        then:
        42 == model.paramValue
    }


    void "test controller with trait action with command params"() {
        given:
        def gcl = createClassLoader()
        def cls = gcl.parseClass('''
class TestTraitActionToController implements ShowMethod {

}

class MyCommandWithArg implements grails.validation.Validateable {

}

trait ShowMethod {

    @grails.web.Action
    def show(MyCommandWithArg myCommandWithArg) {
        !myCommandWithArg.hasErrors()
    }

}
''')

        def controller = cls.newInstance()

        when:
        Boolean valid = controller.show()

        then:
        valid
    }


    void "Test command object gets Validateable injected"() {
        when:
        def gcl = createClassLoader()
        def cls = gcl.parseClass('''
class TestMyCommandObjController {

    def action(MyCommand myCommand) {
    }

    def $test() {
        new MyCommand(name: "Sally")
    }

}

class MyCommand {
    String name
}
''')

        def controller = cls.newInstance()
        def myCommand = controller.$test()

        then:
        controller
        myCommand
        myCommand.validate()
    }

    void "Test command object injected constructor will be marked as Generated"() {
        when:
        def gcl = createClassLoader()
        def cls = gcl.parseClass('''
class TestMyCommandObjController {
    def action(MyCommand myCommand) {
    }

    def $test() {
        new MyCommand(name: "Sally")
    }

}

class MyCommand {
    String name
}
''')

        def controller = cls.newInstance()
        def myCommand = controller.$test()

        then:
        myCommand.getClass().getConstructors().each { Constructor constructor ->
            assert constructor.isAnnotationPresent(Generated)
        }
    }

}


class TestGroovyClassLoader extends GroovyClassLoader {
    private static final List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler)

    CompilationUnit compilationUnit

    private ClassInjector[] classInjectors
    private String artefactType
    boolean enableInjectTraits = true

    TestGroovyClassLoader() {
        // default
    }

    TestGroovyClassLoader(ClassLoader loader) {
        super(loader)
    }

    TestGroovyClassLoader(ClassLoader parent, CompilerConfiguration config, boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath);
    }

    TestGroovyClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config)
    }

    ClassInjector[] getClassInjectors() {
        return classInjectors ?: new ClassInjector[0]
    }

    void setClassInjectors(ClassInjector[] classInjectors) {
        this.classInjectors = classInjectors
    }

    String getArtefactType(ClassNode classNode) {
        if (this.artefactType) {
            return this.artefactType
        }
        String type = null
        if (classNode.name.endsWith('Controller')) {
            type = 'Controller'
        }
        return type
    }

    void setArtefactType(String artefactType) {
        this.artefactType = artefactType
    }

    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit compilationUnit = super.createCompilationUnit(config, source)
        compilationUnit.addPhaseOperation(new CompilationUnit.IPrimaryClassNodeOperation() {

            @Override
            void call(SourceUnit sourceUnit, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                sourceUnit.getAST().putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
                sourceUnit.getAST().putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
                sourceUnit.getAST().putNodeMetaData('PROJECT_TYPE', 'WEB_APP')

                String artefactType = getArtefactType(classNode)
                if (artefactType) {
                    if (!classNode.getAnnotations(ClassHelper.make(Artefact))) {
                        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
                        annotationNode.addMember('value', new ConstantExpression(artefactType))
                        classNode.addAnnotation(annotationNode)
                    }

                    ArtefactTypeAstTransformation.performInjection(sourceUnit, classNode, Arrays.asList(getClassInjectors()))

                    if (enableInjectTraits) {
                        TraitInjectionUtils.processTraitsForNode(sourceUnit, classNode, getArtefactType(classNode), compilationUnit)
                    }
                }
            }

        }, Phases.CANONICALIZATION)

        this.compilationUnit = compilationUnit

        return compilationUnit
    }

    ClassNode getClassNode(String name) {
        this.compilationUnit.getClassNode(name)
    }

}

