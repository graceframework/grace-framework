package grails.boot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import spock.lang.Specification

/**
 * Created by graemerocher on 28/05/14.
 */
class GrailsSpringApplicationSpec extends Specification {

    AnnotationConfigServletWebServerApplicationContext context

    void cleanup() {
        context.close()
    }

    void "Test run Grails via SpringApplication"() {
        when: "SpringApplication is used to run a Grails app"
        GrailsApp grailsApp = new GrailsApp(Application)
        context = (AnnotationConfigServletWebServerApplicationContext) grailsApp.run()

        then: "The application runs"
        context != null
        new URL("http://localhost:${context.webServer.port}/foo/bar").text == 'hello world'
    }


    @SpringBootApplication
    static class Application {
    }
}
