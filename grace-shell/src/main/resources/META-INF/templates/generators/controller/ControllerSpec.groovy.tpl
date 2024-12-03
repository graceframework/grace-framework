package $packageName

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class ${className}ControllerSpec extends Specification implements ControllerUnitTest<${className}Controller> {

    def setup() {
    }

    def cleanup() {
    }

<% actions.each { action -> %>
    void "test ${action}"() {
        expect: "fix me"
            true == false
    }
<% } %>
}
