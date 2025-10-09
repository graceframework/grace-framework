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
package org.grails.compiler.web

import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ClassNode
import org.slf4j.Logger
import spock.lang.Specification

import grails.compiler.ast.ClassInjector

import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.compiler.injection.GrailsAwareInjectionOperation
import org.grails.compiler.logging.LoggingTransformer

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class ControllerLoggingTransformerSpec extends Specification {

    def "Test log field with inheritance and base class with log property"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        gcl.parseClass('''
import org.slf4j.Logger
import org.slf4j.LoggerFactory
class BaseController {
    protected Logger log = LoggerFactory.getLogger(getClass())

}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/BaseController.groovy')

        def cls = gcl.parseClass('''
class LoggingController extends BaseController {
    def index() {
        log.debug "message"
        return log
    }
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/LoggingController.groovy')
        def controller = cls.newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger
    }

    def "Test log field with inheritance"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        gcl.parseClass('''
class BaseController {
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/BaseController.groovy')
        def cls = gcl.parseClass('''

class LoggingController extends BaseController {
    def index() {
        log.debug "message"
        return log
    }
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/LoggingController.groovy')
        def controller = cls.newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger
    }

    def "Test added log field"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def cls = gcl.parseClass('''
class LoggingController {
    def index() {
        log.debug "message"
        return log
    }
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/LoggingController.groovy')
        def controller = cls.newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger
    }

    def "Test adding log field via Artefact annotation"() {
        given:
        def transformer = new LoggingTransformer()
        GrailsAwareInjectionOperation.@classInjectors = [transformer]
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def cls = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class LoggingController {
    def index() {
        log.debug "message"
        return log
    }
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/LoggingController.groovy')
        def controller = cls.newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger

        cleanup:
        GrailsAwareInjectionOperation.@classInjectors = null
    }

    def "Test Controller class was injected on '@Slf4j'"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class PostController {
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/PostController.groovy')

        def classNode = gcl.getClassNode('PostController')

        then:
        !classNode.getAnnotations(new ClassNode(Slf4j))
        classNode.getField("log")
        classNode.getNodeMetaData(Slf4j.class)
    }

    def "Test Controller class was already annotated '@Slf4j'"() {
        given:
        def transformer = new LoggingTransformer()
        def gcl = new GrailsAwareClassLoader()
        gcl.disabledGlobalASTTransformations = true
        gcl.classInjectors = [transformer] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@groovy.util.logging.Slf4j
class PostController {
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/PostController.groovy')

        def classNode = gcl.getClassNode('PostController')

        then:
        classNode.getAnnotations(new ClassNode(Slf4j))
        classNode.getField("log")
        !classNode.getNodeMetaData(Slf4j.class)
    }

}
