package grails.plugin.hibernate

import groovy.transform.CompileStatic
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.ConfigurableConversionService
import org.springframework.core.env.PropertyResolver

import grails.config.Config
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import grails.plugins.Plugin
import grails.util.Environment

import org.grails.config.PropertySourcesConfig
import org.grails.core.artefact.DomainClassArtefactHandler

/**
 * Plugin that integrates Hibernate into a Grails application
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class HibernateGrailsPlugin extends Plugin {

    public static final String DEFAULT_DATA_SOURCE_NAME = HibernateDatastoreSpringInitializer.DEFAULT_DATA_SOURCE_NAME

    def grailsVersion = '2023.0.0 > *'

    def author = 'Grace Framework'
    def title = 'Grace Data Hibernate'
    def description = 'Provides integration between Grace and Hibernate through GORM'
    def documentation = 'https://github.com/graceframework/grace-data-hibernate'

    def observe = ['domainClass']
    def loadAfter = ['controllers', 'domainClass']
    def watchedResources = ['file:./grails-app/conf/hibernate/**.xml', 'file:./app/conf/hibernate/**.xml']
    def pluginExcludes = ['src/templates/**']

    def license = 'APACHE'
    def organization = [name: 'Grace Framework', url: 'https://graceframework.org']
    def issueManagement = [system: 'Github', url: 'https://github.com/graceframework/grace-data-hibernate/issues']
    def scm = [url: 'https://github.com/graceframework/grace-data-hibernate']

    Set<String> dataSourceNames

    Closure doWithSpring() {
        { ->
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) applicationContext

            GrailsApplication grailsApplication = grailsApplication
            Config config = grailsApplication.config
            if (config instanceof PropertySourcesConfig) {
                ConfigurableConversionService conversionService = applicationContext.getEnvironment().getConversionService()
                conversionService.addConverter(new Converter<String, Class>() {

                    @Override
                    Class convert(String source) {
                        Class.forName(source)
                    }
                })
                ((PropertySourcesConfig) config).setConversionService(conversionService)
            }


            def domainClasses = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
                    .collect() { GrailsClass cls -> cls.clazz }

            def springInitializer = new HibernateDatastoreSpringInitializer((PropertyResolver) config, domainClasses)
            springInitializer.enableReload = Environment.isDevelopmentMode()
            springInitializer.registerApplicationIfNotPresent = false
            springInitializer.grailsPlugin = true
            dataSourceNames = springInitializer.dataSources
            def beans = springInitializer.getBeanDefinitions((BeanDefinitionRegistry) applicationContext)

            beans.delegate = delegate
            beans.call()
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        // TODO: rewrite onChange handling
    }

}
