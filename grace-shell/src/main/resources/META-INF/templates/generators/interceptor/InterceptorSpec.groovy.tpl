package $packageName

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class ${className}InterceptorSpec extends Specification implements InterceptorUnitTest<${className}Interceptor> {

    def setup() {
    }

    def cleanup() {
    }

    void "Test ${propertyName} interceptor matching"() {
        when: "A request matches the interceptor"
            withRequest(controller: "${propertyName}")

        then: "The interceptor does match"
            interceptor.doesMatch()
    }

}
