/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.plugins.web.controllers

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.core.PriorityOrdered

import grails.config.Settings
import grails.core.GrailsControllerClass
import grails.plugins.Plugin
import grails.util.GrailsUtil

import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.web.servlet.context.BootStrapClassRunner

/**
 * Handles the configuration of controllers for Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@Slf4j
class ControllersGrailsPlugin extends Plugin implements PriorityOrdered {

    def watchedResources = ['file:./grails-app/controllers/**/*Controller.groovy',
                            'file:./app/controllers/**/*Controller.groovy',
                            'file:./plugins/*/grails-app/controllers/**/*Controller.groovy',
                            'file:./plugins/*/app/controllers/**/*Controller.groovy']

    def version = GrailsUtil.getGrailsVersion()
    def observe = ['domainClass']
    def dependsOn = [core: version, i18n: version]

    @Override
    Closure doWithSpring() {
        { ->
            def application = grailsApplication
            def config = application.config

            boolean useJsessionId = config.getProperty(Settings.GRAILS_VIEWS_ENABLE_JSESSIONID, Boolean, false)

            boolean skipBootStrap = Boolean.parseBoolean(System.getProperty(Settings.SETTING_SKIP_BOOTSTRAP))
            if (!skipBootStrap) {
                bootStrapClassRunner(BootStrapClassRunner)
            }

            def controllerClasses = application.getArtefacts(ControllerArtefactHandler.TYPE)
            for (controller in controllerClasses) {
                log.debug "Configuring controller $controller.fullName"
                if (controller.available) {
                    def lazyInit = controller.hasProperty('lazyInit') ? controller.getPropertyValue('lazyInit') : true
                    "${controller.fullName}"(controller.clazz) { bean ->
                        bean.lazyInit = lazyInit
                        def beanScope = controller.getScope()
                        bean.scope = beanScope
                        bean.autowire = 'byName'
                        if (beanScope == 'prototype') {
                            bean.beanDefinition.dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE
                        }
                        if (useJsessionId) {
                            useJessionId = useJsessionId
                        }
                    }
                }
            }

            if (config.getProperty(Settings.SETTING_LEGACY_JSON_BUILDER, Boolean, false)) {
                log.warn("'grails.json.legacy.builder' is set to TRUE but is NOT supported in this version of Grails.")
            }
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        if (!(event.source instanceof Class)) {
            return
        }
        def application = grailsApplication
        if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, (Class) event.source)) {
            ApplicationContext context = applicationContext
            if (!context) {
                if (log.isDebugEnabled()) {
                    log.debug("Application context not found. Can't reload")
                }
                return
            }

            GrailsControllerClass controllerClass =
                    (GrailsControllerClass) application.addArtefact(ControllerArtefactHandler.TYPE, (Class) event.source)
            beans {
                "${controllerClass.fullName}"(controllerClass.clazz) { bean ->
                    def beanScope = controllerClass.getScope()
                    bean.scope = beanScope
                    bean.autowire = 'byName'
                    if (beanScope == 'prototype') {
                        bean.beanDefinition.dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE
                    }
                }
            }
        }
    }

    @Override
    int getOrder() {
        50
    }

}
