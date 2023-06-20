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

import spock.lang.Specification

import grails.compiler.ast.ClassInjector

/**
 * @author Michael Yan
 * @since 2022.3.0
 */
class DefaultGrailsDomainClassInjectorSpec extends Specification {

    def "Test Domain class was injected Id, Version"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def classInjector = new DefaultGrailsDomainClassInjector()
        gcl.classInjectors = [classInjector] as ClassInjector[]

        def domainClass = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''', "grails-demo-project/grails-app/domain/org/demo/Post.groovy")

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

    def "Test Domain class was injected Associations"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def classInjector = new DefaultGrailsDomainClassInjector()
        gcl.classInjectors = [classInjector] as ClassInjector[]

        gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
    static hasMany = [comments: Comment]
}
@grails.artefact.Artefact("Domain")
class Comment {
    static belongsTo = [post : Post]
}
''', "grails-demo-project/grails-app/domain/org/demo/Post.groovy")

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
