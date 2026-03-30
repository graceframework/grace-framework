package $packageName

import spock.lang.Specification

import grails.testing.web.taglib.TagLibUnitTest

class ${className}TagLibSpec extends Specification implements TagLibUnitTest<${className}TagLib> {

    def setup() {
    }

    def cleanup() {
    }
<% tags.each { tag -> %>
    void 'test ${tag}'() {
        expect: 'fix me'
            true == false
    }
<% } %>
}
