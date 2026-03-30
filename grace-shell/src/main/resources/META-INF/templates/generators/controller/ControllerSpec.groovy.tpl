package $packageName

import spock.lang.Specification

import grails.testing.web.controllers.ControllerUnitTest

class ${className}ControllerSpec extends Specification implements ControllerUnitTest<${className}Controller> {

    def setup() {
    }

    def cleanup() {
    }
<% actions.each { action -> %>
    void 'test ${action}'() {
        expect: 'fix me'
            true == false
    }
<% } %>
}
