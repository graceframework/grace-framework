package org.grails.compiler.boot

import grails.util.Environment
import org.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification

/**
 * Created by graemerocher on 26/08/2016.
 */
class BootInitializerClassInjectorSpec extends Specification {

    void "test compile application class"() {
        when:"An application class is compiled"
        def gcl = new GrailsAwareClassLoader()
        Class applicationClass = gcl.parseClass('''
import grails.boot.Grails
class Application {
    static void main(String[] args) {
        println "foo"
    }
}
''')

        applicationClass.main()

        then:""
        Boolean.getBoolean(Environment.STANDALONE)
        Environment.isStandalone()
        !Environment.isStandaloneDeployed()
    }
}
