package ${packageName}

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration

import geb.spock.*

/**
 * See https://groovy.apache.org/geb/manual/current/ for more instructions
 */
@Integration
@Rollback
class ${className}Spec extends GebSpec {

    def setup() {
    }

    def cleanup() {
    }

    void "test the homepage title"() {
        when: "The homepage is visited"
            go '/'

        then: "The title is correct"
        	title == "Welcome to Grace"
    }
}
