package org.grails.scaffolding.registry.input

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import org.grails.scaffolding.model.property.DomainProperty

@Subject(CurrencyInputRenderer)
class CurrencyInputRendererSpec extends Specification {

    @Shared
    CurrencyInputRenderer renderer

    void setup() {
        renderer = new CurrencyInputRenderer()
    }

    void 'test supports'() {
        given:
        DomainProperty property

        when:
        property = Mock(DomainProperty) {
            1 * getType() >> Currency
        }

        then:
        renderer.supports(property)
    }

    void 'test option key and value'() {
        given:
        Currency currency = Currency.getInstance('USD')

        expect:
        renderer.getOptionKey(currency) == 'USD'
        renderer.getOptionValue(currency) == 'USD'
    }

}
