package $packageName

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class ${className}Spec extends Specification implements DomainUnitTest<${className}> {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        expect:"fix me"
            true == false
    }
}
