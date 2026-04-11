package $packageName

import spock.lang.Specification

import grails.testing.gorm.DomainUnitTest

class ${className}Spec extends Specification implements DomainUnitTest<${className}> {

    def setup() {
    }

    def cleanup() {
    }

    void 'test something'() {
        expect: 'fix me'
        true == false
    }

}
