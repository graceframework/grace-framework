/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.plugins.services

import java.lang.reflect.Modifier

import groovy.util.logging.Slf4j
import org.springframework.core.PriorityOrdered

import grails.config.Settings
import grails.core.GrailsApplication
import grails.core.GrailsServiceClass
import grails.plugins.GrailsPlugin
import grails.plugins.Plugin
import grails.util.GrailsUtil

import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.core.exceptions.GrailsConfigurationException

/**
 * Configures services in the Spring context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@Slf4j
class ServicesGrailsPlugin extends Plugin implements PriorityOrdered {

    def version = GrailsUtil.getGrailsVersion()
    def loadAfter = ['hibernate']

    def watchedResources = ['file:./grails-app/services/**/*Service.groovy',
                            'file:./plugins/*/grails-app/services/**/*Service.groovy',
                            'file:./app/services/**/*Service.groovy',
                            'file:./plugins/*/app/services/**/*Service.groovy']

    Closure doWithSpring() {
        { ->
            GrailsApplication application = grailsApplication
            boolean springTransactionManagement = config.getProperty(Settings.SPRING_TRANSACTION_MANAGEMENT, Boolean, false)
            if (springTransactionManagement) {
                throw new GrailsConfigurationException('Spring proxy-based transaction management no longer supported.' +
                        'Yes the @grails.gorm.transactions.Transactional annotation instead')
            }

            def serviceClasses = application.getArtefacts(ServiceArtefactHandler.TYPE)
            for (GrailsServiceClass serviceClass in serviceClasses) {
                GrailsPlugin providingPlugin = manager?.getPluginForClass(serviceClass.clazz)

                String beanName
                if (providingPlugin && !serviceClass.shortName.toLowerCase().startsWith(providingPlugin.name.toLowerCase())) {
                    beanName = "${providingPlugin.name}${serviceClass.shortName}"
                }
                else {
                    beanName = serviceClass.propertyName
                }
                def scope = serviceClass.getPropertyValue('scope')
                def lazyInit = serviceClass.hasProperty('lazyInit') ? serviceClass.getPropertyValue('lazyInit') : true

                "${beanName}"(serviceClass.getClazz()) { bean ->
                    bean.autowire = true
                    if (lazyInit instanceof Boolean) {
                        bean.lazyInit = lazyInit
                    }
                    if (scope) {
                        bean.scope = scope
                    }
                }
            }

            serviceBeanAliasPostProcessor(ServiceBeanAliasPostProcessor)
        }
    }

    void onChange(Map<String, Object> event) {
        if (!event.source || !applicationContext) {
            return
        }

        if (event.source instanceof Class) {
            def application = grailsApplication
            Class javaClass = event.source
            // do nothing for abstract classes
            if (Modifier.isAbstract(javaClass.modifiers)) {
                return
            }
            def serviceClass = (GrailsServiceClass) application.addArtefact(ServiceArtefactHandler.TYPE, (Class) event.source)
            def serviceName = "${serviceClass.propertyName}"
            def scope = serviceClass.getPropertyValue('scope')

            beans {
                "$serviceName"(serviceClass.getClazz()) { bean ->
                    bean.autowire = true
                    if (scope) {
                        bean.scope = scope
                    }
                }
            }
        }
    }

    @Override
    int getOrder() {
        500
    }

}
