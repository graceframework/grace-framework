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

import org.springframework.boot.autoconfigure.SpringBootApplication
import spock.lang.Specification

import grails.compiler.ast.ClassInjector

/**
 * @author Michael Yan
 * @since 2022.3.0
 */
class ApplicationClassInjectorSpec extends Specification {

    def "Test Application class was injected on '@SpringBootApplication'"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ApplicationClassInjector()
        gcl.classInjectors = [transformer] as ClassInjector[]

        when:
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class Application {
}
''', "foo/app/init/org/demo/Application.groovy")

        then:
        clazz.getAnnotationsByType(SpringBootApplication).size() == 1
    }

    def "Test Application class NOT injected on '@SpringBootApplication'"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ApplicationClassInjector()
        gcl.classInjectors = [transformer] as ClassInjector[]

        when:
        def clazz = gcl.parseClass('''
class Application {
}
''', "foo/app/init/org/demo/Application.groovy")

        then:
        !clazz.getAnnotationsByType(SpringBootApplication)
    }

}
