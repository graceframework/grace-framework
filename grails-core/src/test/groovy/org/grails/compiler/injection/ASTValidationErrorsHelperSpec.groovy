package org.grails.compiler.injection

import groovy.transform.Generated
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import grails.compiler.ast.ClassInjector
import org.springframework.validation.Errors

import spock.lang.Specification

import java.lang.reflect.Method

class ASTValidationErrorsHelperSpec extends Specification {

    def createGrailsClassLoader() {
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]
        def transformer = new ClassInjector() {
            @Override
            void performInjection(SourceUnit source, ClassNode classNode) {
                performInjection(source, null, classNode)
            }

            @Override
            void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
                new ASTValidationErrorsHelper().injectErrorsCode(classNode)
            }
            @Override
            boolean shouldInject(ClassNode classNode) { true }
        }
        gcl.classInjectors = [transformer] as ClassInjector[]
        return gcl
    }

    void 'Test injected errors property'() {
        given:
            def gcl = createGrailsClassLoader()
            def widgetClass = gcl.parseClass('''
class MyWidget {
}
''', '/Users/grails/grails-demo-project/grails-app/domain/org/demo/MyWidget.groovy')

        when:
            def widget = widgetClass.newInstance()

        then:
            !widget.hasErrors()

        when:
            widget.setErrors([hasErrors: { false }] as Errors)

        then:
            !widget.hasErrors()

        when:
            widget.setErrors([hasErrors: { true }] as Errors)

        then:
            widget.hasErrors()

        when:
            widget.clearErrors()

        then:
            !widget.hasErrors()

        when:
            def localErrors = [:] as Errors
            widget.setErrors(localErrors)

        then:
            localErrors.is(widget.getErrors())
    }

    void 'Test injected errors property methods are marked with Generated annotation'() {
        given:
        def gcl = createGrailsClassLoader()
        def widgetClass = gcl.parseClass('class MyWidget{}')

        and: 'injected method names to it'
        List<String> injectedMethodNames = [
            "setErrors",
            "getErrors",
            "hasErrors",
            "clearErrors",
            "initErrors"
        ]

        expect: 'injected methods marked as Generated'
        widgetClass.getMethods().each { Method widgetMethod ->
            if (widgetMethod.name in injectedMethodNames) {
                assert widgetMethod.isAnnotationPresent(Generated)
            }
        }
    }
}
