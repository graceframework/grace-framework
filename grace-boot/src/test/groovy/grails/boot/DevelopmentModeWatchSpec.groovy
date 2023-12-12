package grails.boot

import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.Plugin
import grails.util.Environment
import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

/**
 * @author Anders Aaberg
 */
class DevelopmentModeWatchSpec extends Specification {

    void "test root watchPattern"() {
        setup:
        System.setProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
        System.setProperty("base.dir", ".")
        Grails app = new Grails(GrailsAutoConfiguration.class)
        app.webApplicationType = WebApplicationType.NONE
        ConfigurableApplicationContext context = app.run("--server.port=0")
        WatchedResourcesGrailsPlugin plugin = context.getBean('pluginManager').getGrailsPlugin('watchedResources').instance
        PollingConditions pollingCondition = new PollingConditions(timeout: 10, initialDelay: 5, factor: 1)

        when:
        File watchedFile = new File("build",'testWatchedFile.properties')
        watchedFile.createNewFile()
        watchedFile.write 'foo.bar=baz'

        then:
        pollingCondition.eventually {
            assert plugin.fileIsChanged.endsWith('testWatchedFile.properties')
        }

        cleanup:
        System.clearProperty("base.dir")
        System.setProperty(Environment.KEY, Environment.TEST.getName())
        if(watchedFile != null) {
            watchedFile.delete()
        }
    }
}

class WatchedResourcesGrailsPlugin extends Plugin {
    def version = "1.0"
    def watchedResources = "file:./**/*.properties"

    void onChange(Map<String, Object> event) {
        fileIsChanged = event.source.path.toString()
    }
    String fileIsChanged = ""
}
