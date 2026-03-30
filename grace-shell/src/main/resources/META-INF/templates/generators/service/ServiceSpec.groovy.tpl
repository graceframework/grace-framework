package $packageName

import spock.lang.Specification

import grails.testing.services.ServiceUnitTest

class ${className}ServiceSpec extends Specification implements ServiceUnitTest<${className}Service> {

    def setup() {
    }

    def cleanup() {
    }
<% methods.each { method -> %>
    void 'test ${method}'() {
        expect: 'fix me'
            true == false
    }
<% } %>
}
