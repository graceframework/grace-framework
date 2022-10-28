package org.grails.config

import org.grails.testing.GrailsUnitTest
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

class GroovyConfigPropertySourceLoaderSpec extends Specification implements GrailsUnitTest {

    void "test read config from application.groovy"() {

        expect:
        ((ConfigurableApplicationContext) applicationContext).getEnvironment().getProperty("foo", String) == "bar"
    }
}
