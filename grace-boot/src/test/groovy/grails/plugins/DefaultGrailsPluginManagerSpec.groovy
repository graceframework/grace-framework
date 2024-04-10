package grails.plugins

import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

import grails.boot.Grails
import grails.boot.config.GrailsAutoConfiguration
import grails.util.Environment

class DefaultGrailsPluginManagerSpec extends Specification {

    void "test root watchPattern"() {
        setup:
        System.setProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
        Grails app = new Grails(GrailsAutoConfiguration.class)
        app.webApplicationType = WebApplicationType.NONE
        ConfigurableApplicationContext context = app.run("--server.port=0")
        List<GrailsPlugin> pluginList = context.getBean('pluginManager').getPluginList()

        expect:
        pluginList.size() >= 5
        pluginList[0].name == 'core'
        pluginList[0].order == 0
        pluginList[1].name == 'i18n'
        pluginList[1].order == 20
        pluginList[2].name == 'controllers'
        pluginList[2].order == 50
        pluginList[3].name == 'converters'
        pluginList[3].order == 60
        pluginList[4].name == 'urlMappings'
        pluginList[4].order == 70

        cleanup:
        System.setProperty(Environment.KEY, Environment.TEST.getName())
    }
}
