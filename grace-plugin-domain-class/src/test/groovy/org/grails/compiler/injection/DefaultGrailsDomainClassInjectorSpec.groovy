/*
 * Copyright 2022-2026 the original author or authors.
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

import groovy.transform.Generated
import spock.lang.Specification

import grails.compiler.ast.ClassInjector

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class DefaultGrailsDomainClassInjectorSpec extends Specification {

    def "Test Domain class was injected Id, Version"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        def classInjector = new DefaultGrailsDomainClassInjector()
        gcl.classInjectors = [classInjector] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]
        def domainClass = gcl.parseClass('''
class Post {
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'domain', 'org', 'demo', 'Post.groovy'].join(File.separator))

        def domainMethodNames = domainClass.getMethods()*.name

        and:
        List<String> injectedMethodNames = [
                "setId",
                "getId",
                "setVersion",
                "getVersion",
                "toString"
        ]

        expect: 'injected methods as expect'
        injectedMethodNames.each { methodName ->
            assert methodName in domainMethodNames
        }
    }

    def "Test Domain class with default ToString"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        def classInjector = new DefaultGrailsDomainClassInjector()
        gcl.classInjectors = [classInjector] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]

        def domainClass = gcl.parseClass('''
class Post {
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'domain', 'org', 'demo', 'Post.groovy'].join(File.separator))

        def domainMethodNames = domainClass.getMethods()*.name

        when:
        List<String> injectedMethodNames = [
                "setId",
                "getId",
                "setVersion",
                "getVersion",
                "toString"
        ]

        then: 'injected methods as expect'
        injectedMethodNames.each { methodName ->
            assert methodName in domainMethodNames
        }

        when:
        def post = domainClass.newInstance([id: 1])

        then: 'toString as default'
        post.toString() == "Post : 1"

        and: 'toString is marked as Generated'
        post.class.getMethod('toString').isAnnotationPresent(Generated)
    }

    def "Test Domain class annotated with '@groovy.transform.ToString'"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        def classInjector = new DefaultGrailsDomainClassInjector()
        gcl.classInjectors = [classInjector] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]

        def domainClass = gcl.parseClass('''
@grails.persistence.Entity
@groovy.transform.ToString(includes = ["id"])
class Post {
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'domain', 'org', 'demo', 'Post.groovy'].join(File.separator))

        def domainMethodNames = domainClass.getMethods()*.name

        when:
        List<String> injectedMethodNames = [
                "setId",
                "getId",
                "setVersion",
                "getVersion",
                "toString"
        ]

        then: 'injected methods as expect'
        injectedMethodNames.each { methodName ->
            assert methodName in domainMethodNames
        }

        when:
        def post = domainClass.newInstance([id: 1])

        then: 'toString as expect'
        post.toString().endsWith("Post(1)")

        and: 'toString is marked as Generated'
        post.class.getMethod('toString').isAnnotationPresent(Generated)
    }

    def "Test Domain class was injected Associations"() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        def classInjector = new DefaultGrailsDomainClassInjector()
        gcl.classInjectors = [classInjector] as ClassInjector[]
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]

        gcl.parseClass('''
class Post {
    static hasMany = [comments: Comment]
}
class Comment {
    static belongsTo = [post : Post]
}
''', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'domain', 'org', 'demo', 'Post.groovy'].join(File.separator))

        Class[] loadedClasses = gcl.getLoadedClasses()
        def postClass = loadedClasses.find { it.name == 'Post' }
        def commentClass = loadedClasses.find { it.name == 'Comment' }

        def postMethodNames = postClass.getMethods()*.name
        def commentMethodNames = commentClass.getMethods()*.name

        expect: 'Domain classes injected association methods'
        postMethodNames.containsAll('getComments', 'setComments')
        commentMethodNames.containsAll('getPost', 'setPost')
    }

}
