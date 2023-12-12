package grails.spring

import grails.core.DefaultGrailsApplication
import spock.lang.Issue
import spock.lang.Specification

import org.grails.spring.context.support.GrailsPlaceholderConfigurer

/**
 * @author Graeme Rocher
 */
class GrailsPlaceHolderConfigurerCorePluginRuntimeSpec extends Specification{

    @Issue('GRAILS-10130')
    void "Test that system properties are used to replace values at runtime with GrailsPlaceHolderConfigurer"() {
        given:"A configured application context"
            def parent = new BeanBuilder()
            parent.beans {
                grailsApplication(DefaultGrailsApplication)
            }
            def bb = new BeanBuilder(parent.createApplicationContext())

            final beanBinding = new Binding()

            def app = new DefaultGrailsApplication()
            beanBinding.setVariable('application', app)
            bb.setBinding(beanBinding)

            bb.beans {
                propertySourcesPlaceholderConfigurer(GrailsPlaceholderConfigurer, '${', app.config.toProperties())
                testBean(ReplacePropertyBean) {
                    foo = '${foo.bar}'
                }
            }

        when:"A system property is used in a bean property"
            System.setProperty('foo.bar', "test")
            final appCtx = bb.createApplicationContext()
            def bean = appCtx.getBean("testBean")

        then:"The system property is ready"
            appCtx != null
            bean.foo == 'test'
    }

}
class ReplacePropertyBean {
    String foo
}

