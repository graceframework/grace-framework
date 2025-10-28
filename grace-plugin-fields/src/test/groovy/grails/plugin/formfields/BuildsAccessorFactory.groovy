package grails.plugin.formfields

import spock.lang.Specification

import grails.core.support.proxy.DefaultProxyHandler
import grails.testing.gorm.DataTest
import grails.testing.web.GrailsWebUnitTest

import org.grails.datastore.mapping.model.MappingContext
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl

/**
 * Created by jameskleeh on 5/3/17.
 */
abstract class BuildsAccessorFactory extends Specification implements GrailsWebUnitTest, DataTest {

    void setupSpec() {
        defineBeans { ->
            def domainClassMappingContext = applicationContext.getBean('grailsDomainClassMappingContext', MappingContext)
            def domainPropertyFactory = new DomainPropertyFactoryImpl(domainClassMappingContext)

            beanPropertyAccessorFactory(BeanPropertyAccessorFactory,
                    grailsApplication,
                    domainClassMappingContext,
                    ref('validateableConstraintsEvaluator'),
                    domainPropertyFactory, new DefaultProxyHandler())
        }
    }

    BeanPropertyAccessorFactory getFactory() {
        applicationContext.getBean(BeanPropertyAccessorFactory)
    }

}
