/*
 * Copyright 2016-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.hibernate

import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.ConfigurableConversionService

import grails.config.Config
import grails.core.GrailsApplication
import grails.plugins.Plugin

import org.grails.config.PropertySourcesConfig

/**
 * Plugin that integrates Hibernate into a Grails application
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class HibernateGrailsPlugin extends Plugin {

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
        }
    }

}
