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
package org.grails.compiler.boot

import spock.lang.Specification

import grails.compiler.ast.ClassInjector
import grails.util.Environment
import org.grails.compiler.injection.GrailsAwareClassLoader

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
class BootInitializerClassInjectorSpec extends Specification {

    void "Test Application set 'grails.env.standalone' property"() {
        when: "An application class is compiled"
        def gcl = new GrailsAwareClassLoader()
        gcl.setDisabledGlobalASTTransformations(true)
        gcl.setMetaDataMap([
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ])
        Class applicationClass = gcl.parseClass('''
import grails.boot.Grails

class Application {
    static void main(String[] args) {
        println "foo"
    }
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Application.groovy')

        applicationClass.main()

        then: ""
        Boolean.getBoolean(Environment.STANDALONE)
        Environment.isStandalone()
        !Environment.isStandaloneDeployed()
    }

    def "Test ApplicationLoader class was generated with Application in 'app/boot'"() {
        given:
        def transformer = new BootInitializerClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.setDisabledGlobalASTTransformations(true)
        gcl.setClassInjectors([transformer] as ClassInjector[])
        gcl.setMetaDataMap([
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ])

        when:
        def clazz = gcl.parseClass('''
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        applicationLoader
    }

    def "Test ApplicationLoader class was generated with Application in 'src/main'"() {
        given:
        def transformer = new BootInitializerClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.setDisabledGlobalASTTransformations(true)
        gcl.setClassInjectors([transformer] as ClassInjector[])
        gcl.setMetaDataMap([
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ])

        when:
        def clazz = gcl.parseClass('''
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/src/main/groovy/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        applicationLoader
    }

    def "Test ApplicationLoader class was NOT generated in Plugin"() {
        given:
        def transformer = new BootInitializerClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.setDisabledGlobalASTTransformations(true)
        gcl.setClassInjectors([transformer] as ClassInjector[])
        gcl.setMetaDataMap([
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'WEB_APP'
        ])

        when:
        def clazz = gcl.parseClass('''
@grails.plugins.metadata.PluginSource
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        !applicationLoader
    }

    def "Test ApplicationLoader class was NOT generated in Plugin too"() {
        given:
        def transformer = new BootInitializerClassInjector()
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.setDisabledGlobalASTTransformations(true)
        gcl.setClassInjectors([transformer] as ClassInjector[])
        gcl.setMetaDataMap([
                'GRAILS_APP_DIR': '/Users/grails/grails-demo-project/grails-app',
                'PROJECT_DIR': '/Users/grails/grails-demo-project',
                'PROJECT_TYPE': 'PLUGIN'
        ])

        when:
        def clazz = gcl.parseClass('''
class Application {
    static void main(String[] args) {
    }
}
''', '/Users/grails/grails-demo-project/grails-app/boot/org/demo/Application.groovy')
        def applicationLoader = gcl.getClassNode('ApplicationLoader')

        then:
        !applicationLoader
    }

}
