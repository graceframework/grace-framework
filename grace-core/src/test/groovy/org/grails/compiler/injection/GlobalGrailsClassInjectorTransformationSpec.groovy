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
package org.grails.compiler.injection

import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import grails.artefact.Artefact

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class GlobalGrailsClassInjectorTransformationSpec extends Specification {

    def "Test Application class was applied"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = false
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Application {
}
''', '/Users/grails/grails-demo-project/grails-app/init/org/demo/Application.groovy')

        def classNode = gcl.getClassNode('Application')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.ApplicationClassInjector')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.boot.BootInitializerClassInjector')
    }

    def "Test Bootstrap class was applied"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = false
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Bootstrap {
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Bootstrap.groovy')

        def classNode = gcl.getClassNode('Bootstrap')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
    }

    def "Test Controller class was applied"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = false
        gcl.disabledGrailsAwareInjectionOperation = true
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
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerActionTransformer')
    }

    def "Test Controller class was annotated on '@grails.web.Controller'"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = false
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
@grails.web.Controller
class PostController {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/PostController.groovy')

        def classNode = gcl.getClassNode('PostController')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerActionTransformer')
    }

    def "Test Domain class was applied"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = false
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Post {
}
''', '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy')

        def classNode = gcl.getClassNode('Post')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.DefaultGrailsDomainClassInjector')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.converters.ConvertersDomainTransformer')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerDomainTransformer')
    }

    def "Test Domain class was not annotated on '@Artefact' as GlobalGrailsClassInjectorTransformation disabled"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Post {
}
''', '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy')

        def classNode = gcl.getClassNode('Post')

        then:
        !clazz.getAnnotationsByType(Artefact)
        !classNode.getAnnotations(ClassHelper.make(Artefact))
        !classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.DefaultGrailsDomainClassInjector')
        !classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.converters.ConvertersDomainTransformer')
        !classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerDomainTransformer')
    }

    def "Test Domain class was annotated on '@grails.persistence.Entity'"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true

        when:
        def clazz = gcl.parseClass('''
@grails.persistence.Entity
class Post {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/Post.groovy')

        def classNode = gcl.getClassNode('Post')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.injection.DefaultGrailsDomainClassInjector')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.converters.ConvertersDomainTransformer')
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.ControllerDomainTransformer')
    }

    def "Test TagLib class was applied"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = false
        gcl.disabledGrailsAwareInjectionOperation = true
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class PostTagLib {
}
''', '/Users/grails/grails-demo-project/grails-app/taglib/org/demo/PostTagLib.groovy')

        def classNode = gcl.getClassNode('PostTagLib')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.taglib.TagLibraryTransformer')
    }

    def "Test TagLib class was annotated on '@grails.gsp.TagLib'"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true

        when:
        def clazz = gcl.parseClass('''
@grails.gsp.TagLib
class PostTagLib {
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/PostTagLib.groovy')

        def classNode = gcl.getClassNode('PostTagLib')

        then:
        clazz.getAnnotationsByType(Artefact)
        classNode.getAnnotations(ClassHelper.make(Artefact))
        classNode.getNodeMetaData('APPLIED_org.grails.compiler.web.taglib.TagLibraryTransformer')
    }

}

