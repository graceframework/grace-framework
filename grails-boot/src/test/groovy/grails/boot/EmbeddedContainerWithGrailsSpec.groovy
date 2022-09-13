package grails.boot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import spock.lang.Specification

import grails.artefact.Artefact
import grails.boot.config.GrailsAutoConfiguration
import grails.web.Controller

/**
 * Created by graemerocher on 28/05/14.
 */
class EmbeddedContainerWithGrailsSpec extends Specification {

    AnnotationConfigServletWebServerApplicationContext context

    void cleanup() {
        context.close()
    }

    void "Test that you can load Grails in an embedded server config"() {
        when: "An embedded server config is created"
        this.context = new AnnotationConfigServletWebServerApplicationContext(Application)

        then: "The context is valid"
        context != null
        new URL("http://localhost:${context.webServer.port}/foo/bar").text == 'hello world'
        new URL("http://localhost:${context.webServer.port}/foos").text == 'all foos'
    }

    @SpringBootApplication
    static class Application extends GrailsAutoConfiguration {
    }

}

@Artefact("Controller")
@Controller
class FooController {

    def bar() {
        render "hello world"
    }

    def list() {
        render "all foos"
    }

    def closure = {}
}

@Artefact('UrlMappings')
class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"()
        "/foos"(controller: 'foo', action: "list")
    }
}

