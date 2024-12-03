package $packageName

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class ${className}ServiceSpec extends Specification implements ServiceUnitTest<${className}Service> {

    def setup() {
    }

    def cleanup() {
    }
<% methods.each { method -> %>
    void "test ${method}"() {
        expect: "fix me"
            true == false
    }
<% } %>
}
