package grails.plugin.cache.util

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Jeff Brown
 */
class ClassUtilsSpec extends Specification {

    @Unroll("#fieldOrPropertyName value should have been #expectedValue")
    def 'Test retrieving field or property value'() {
        given:
        def obj = new SomeGroovyClass()

        expect:
        expectedValue == ClassUtils.getPropertyOrFieldValue(obj, fieldOrPropertyName)

        where:
        fieldOrPropertyName        | expectedValue
        'someProperty'             | 1
        'publicField'              | 2
        'privateField'             | 3
        'propertyWithPrivateField' | 100
    }
}

class SomeGroovyClass {

    public publicField = 2
    private int privateField = 3
    private propertyWithPrivateField = 4
    def getSomeProperty() {
        1
    }

    def getPropertyWithPrivateField() {
        100
    }
}
