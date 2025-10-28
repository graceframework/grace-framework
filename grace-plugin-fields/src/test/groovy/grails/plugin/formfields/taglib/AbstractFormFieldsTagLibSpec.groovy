package grails.plugin.formfields.taglib

import spock.lang.Specification

import grails.core.support.proxy.DefaultProxyHandler
import grails.plugin.formfields.BeanPropertyAccessorFactory
import grails.plugin.formfields.mock.Address
import grails.plugin.formfields.mock.Gender
import grails.plugin.formfields.mock.Person
import grails.plugin.formfields.mock.Product
import grails.testing.gorm.DataTest
import grails.testing.web.GrailsWebUnitTest

import org.grails.datastore.mapping.model.MappingContext
import org.grails.plugins.web.DefaultGrailsTagDateHelper
import org.grails.scaffolding.model.DomainModelServiceImpl
import org.grails.scaffolding.model.property.DomainPropertyFactory
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl
import org.grails.spring.beans.factory.InstanceFactoryBean

abstract class AbstractFormFieldsTagLibSpec extends Specification implements GrailsWebUnitTest, DataTest {

    Person personInstance
    Product productInstance

    def setup() {
        personInstance = new Person(name: 'Bart Simpson', password: 'bartman', gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
        personInstance.address = new Address(street: '94 Evergreen Terrace', city: 'Springfield', country: 'USA')
        personInstance.emails = [home: 'bart@thesimpsons.net', school: 'bart.simpson@springfieldelementary.edu']
        productInstance = new Product(netPrice: 12.33, name: "<script>alert('XSS');</script>")
    }

    def cleanup() {
        views.clear()
        applicationContext.getBean('groovyPagesTemplateEngine').clearPageCache()
        applicationContext.getBean('groovyPagesTemplateRenderer').clearCache()

        messageSource.@messageMap.clear() // bit of a hack but messages don't get torn down otherwise
    }

    void setupSpec() {
        defineBeans { ->
            grailsTagDateHelper(DefaultGrailsTagDateHelper)
            //constraintsEvaluator(DefaultConstraintEvaluator)
            def domainClassMappingContext = applicationContext.getBean('grailsDomainClassMappingContext', MappingContext)
            def dpf = new DomainPropertyFactoryImpl(domainClassMappingContext)
            domainPropertyFactory(InstanceFactoryBean, dpf, DomainPropertyFactory)

            domainModelService(DomainModelServiceImpl) {
                domainPropertyFactory = ref('domainPropertyFactory')
            }
            beanPropertyAccessorFactory(BeanPropertyAccessorFactory,
                    grailsApplication,
                    domainClassMappingContext,
                    ref('validateableConstraintsEvaluator'),
                    ref('domainPropertyFactory'), new DefaultProxyHandler())
        }
    }

    protected void mockEmbeddedSitemeshLayout(taglib) {
        taglib.metaClass.applyLayout = { Map attrs, Closure body ->
            if (attrs.name == '_fields/embedded') {
                out << '<fieldset class="embedded ' << attrs.params.type << '">'
                out << '<legend>' << attrs.params.legend << '</legend>'
                out << body()
                out << '</fieldset>'
            }
            null // stops default return
        }
    }

}
