package org.grails.plugins

import grails.boot.Grails
//import io.micronaut.spring.context.env.MicronautEnvironment
import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

class PluginYamlPropertySourceLoaderSpec extends Specification {

    void setup() {
        GrailsPluginConfigurationClass.GROOVY_EXISTS = false
        GrailsPluginConfigurationClass.YAML_EXISTS = true
    }

//    void "test load plugin.yml configuration from the plugin to the Micronaut context"() {
//
//        given:
//        Grails app = new Grails(GrailsPluginConfigurationClass.class)
//        app.setWebApplicationType(WebApplicationType.NONE)
//        ConfigurableApplicationContext context = app.run()
//
//        expect:
//        ((MicronautEnvironment) context.parent.getEnvironment()).getProperty("bar", String.class) == 'foo'
//    }
//
//    void "test load plugin.yml configuration from application overrides the one from plugin to the Micronaut context"() {
//
//        given:
//        Grails app = new Grails(GrailsPluginConfigurationClass.class)
//        app.setWebApplicationType(WebApplicationType.NONE)
//        ConfigurableApplicationContext context = app.run()
//        MicronautEnvironment environment = (MicronautEnvironment) context.parent.getEnvironment()
//
//        expect:
//        environment.getProperty("bar", String.class) == 'foo'
//        environment.getProperty("foo", String.class) == 'foobar'
//        environment.getProperty("abc", String.class) == 'xyz'
//    }
//
//    void "test load plugin.yml configuration to the Micronaut context base on the Grails plugin loadAfter"() {
//
//        given:
//        Grails app = new Grails(GrailsPluginConfigurationClass.class)
//        app.setWebApplicationType(WebApplicationType.NONE)
//        ConfigurableApplicationContext context = app.run()
//        MicronautEnvironment environment = (MicronautEnvironment) context.parent.getEnvironment()
//
//        expect:
//        environment.getProperty("bar", String.class) == 'foo'
//        environment.getProperty("abc", String.class) == 'xyz'
//    }
}
