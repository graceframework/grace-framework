/*
 * Copyright 2016-2022 the original author or authors.
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
package org.grails.testing

import java.lang.reflect.Method

import groovy.transform.CompileStatic
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.MessageSource
import org.springframework.util.ClassUtils

import grails.config.Config
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.spring.BeanBuilder
import grails.util.Holders
import grails.validation.DeferredBindingActions

import org.grails.core.lifecycle.ShutdownOperations

@CompileStatic
trait GrailsUnitTest {

    private static GrailsApplication grailsApplicationInstance
    private static Object servletContextInstance

    boolean getLocalOverride() {
        false
    }

    /**
     * @return the servlet context
     */
    Object getOptionalServletContext() {
        servletContextInstance
    }

    /**
     *
     * @return grailsApplication.mainContext
     */
    ConfigurableApplicationContext getApplicationContext() {
        (ConfigurableApplicationContext) grailsApplication.mainContext
    }

    /**
     *
     * @return The GrailsApplication instance
     */
    GrailsApplication getGrailsApplication() {
        if (grailsApplicationInstance == null) {
            def builder = new GrailsApplicationBuilder(
                    doWithSpring: doWithSpring(),
                    doWithConfig: doWithConfig(),
                    includePlugins: getIncludePlugins(),
                    loadExternalBeans: loadExternalBeans(),
                    localOverride: localOverride
            ).build()
            grailsApplicationInstance = builder.grailsApplication
            servletContextInstance = builder.servletContext
        }
        grailsApplicationInstance
    }

    /**
     *
     * @return grailsApplication.config
     */
    Config getConfig() {
        grailsApplication.config
    }

    /**
     *
     * @return the MessageSource bean from the application context
     */
    MessageSource getMessageSource() {
        applicationContext.getBean('messageSource', MessageSource)
    }

    void defineBeans(Closure closure) {
        def binding = new Binding()
        def bb = new BeanBuilder(null, null, grailsApplication.getClassLoader())
        binding.setVariable 'application', grailsApplication
        bb.setBinding binding
        bb.beans(closure)
        bb.registerBeans((BeanDefinitionRegistry) applicationContext)
        applicationContext.beanFactory.preInstantiateSingletons()
    }

    void defineBeans(Object plugin) {
        Class clazz = plugin.getClass()
        try {
            Method doWithSpringMethod = clazz.getMethod('doWithSpring')
            Closure config = (Closure) doWithSpringMethod.invoke(plugin)
            if (config != null) {
                defineBeans(config)
                return
            }
        }
        catch (NoSuchMethodException ignore) {
        }

        try {
            Method doWithSpringField = clazz.getMethod('getDoWithSpring')
            defineBeans((Closure) doWithSpringField.invoke(plugin))
        }
        catch (NoSuchFieldException ignore) {
        }
    }

    Closure doWithSpring() {
        null
    }

    Closure doWithConfig() {
        null
    }

    Set<String> getIncludePlugins() {
        new HashSet<String>()
    }

    boolean loadExternalBeans() {
        false
    }

    void cleanupGrailsApplication() {
        if (grailsApplicationInstance != null) {
            if (grailsApplicationInstance instanceof DefaultGrailsApplication) {
                ((DefaultGrailsApplication) grailsApplicationInstance).clear()
            }

            ApplicationContext applicationContext = grailsApplication.getParentContext()

            if (applicationContext instanceof ConfigurableApplicationContext) {
                if (((ConfigurableApplicationContext) applicationContext).isActive()) {
                    if (grailsApplication.mainContext instanceof Closeable) {
                        ((Closeable) grailsApplication.mainContext).close()
                    }
                    if (applicationContext instanceof Closeable) {
                        ((Closeable) applicationContext).close()
                    }
                }
            }

            ShutdownOperations.runOperations()
            DeferredBindingActions.clear()

            grailsApplicationInstance = null
            cleanupPromiseFactory()
            Holders.clear()
        }
    }

    private void cleanupPromiseFactory() {
        ClassLoader classLoader = getClass().classLoader
        if (ClassUtils.isPresent('grails.async.Promises', classLoader)) {
            grails.async.Promises.promiseFactory = null
        }
    }

}
