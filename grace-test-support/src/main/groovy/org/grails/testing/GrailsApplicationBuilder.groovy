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
package org.grails.testing

import groovy.transform.CompileDynamic
import org.springframework.beans.BeansException
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.AnnotationConfigUtils
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.Ordered
import org.springframework.util.ClassUtils

import grails.boot.config.GrailsApplicationPostProcessor
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import grails.core.GrailsApplicationLifeCycle
import grails.core.support.proxy.DefaultProxyHandler
import grails.plugins.GrailsPluginManager
import grails.spring.BeanBuilder
import grails.util.Holders

import org.grails.plugins.IncludingPluginFilter
import org.grails.plugins.codecs.CodecsPluginConfiguration
import org.grails.plugins.converters.ConvertersAutoConfiguration
import org.grails.plugins.core.CoreConfiguration
import org.grails.plugins.databinding.DataBindingConfiguration
import org.grails.plugins.web.mime.MimeTypesConfiguration
import org.grails.spring.context.support.GrailsBeanPropertyOverrideConfigurer
import org.grails.spring.context.support.GrailsPlaceholderConfigurer
import org.grails.transaction.TransactionManagerPostProcessor

/**
 * Created by jameskleeh on 5/31/17.
 */
class GrailsApplicationBuilder {

    private static final boolean IS_SERVLET_API_PRESENT =
            ClassUtils.isPresent('jakarta.servlet.ServletContext', GrailsApplicationBuilder.classLoader)

    static final Set DEFAULT_INCLUDED_PLUGINS = ['core', 'eventBus'] as Set
    static final Class[] DEFAULT_AUTO_CONFIGURATIONS = [
            GrailsAutoConfiguration,
            CoreConfiguration,
            ConvertersAutoConfiguration,
            CodecsPluginConfiguration,
            DataBindingConfiguration,
            MimeTypesConfiguration]

    Closure doWithSpring
    Closure doWithConfig
    Set<String> includePlugins
    boolean loadExternalBeans
    boolean localOverride = false

    GrailsApplication grailsApplication
    Object servletContext

    GrailsApplicationBuilder build() {
        servletContext = createServletContext()
        ConfigurableApplicationContext mainContext = createMainContext(servletContext)

        if (IS_SERVLET_API_PRESENT) {
            // NOTE: The following dynamic class loading hack is temporary so the
            // compile time dependency on the servlet api can be removed from this
            // sub project.  This whole GrailsApplicationTestPlugin class will soon
            // be removed so rather than implement a real solution, this hack will
            // do for now to keep the build healthy.
            try {
                Class segads = Class.forName('org.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy')
                Holders.addApplicationDiscoveryStrategy(segads.newInstance(servletContext))
            }
            catch (Throwable ignore) {
            }
            try {
                Class gcu = Class.forName('org.grails.web.servlet.context.GrailsConfigUtils')
                gcu.configureServletContextAttributes(servletContext, grailsApplication,
                        mainContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager), mainContext)
            }
            catch (Throwable ignore) {
            }
        }

        grailsApplication = mainContext.getBean('grailsApplication')

        if (!grailsApplication.isInitialised()) {
            grailsApplication.initialise()
        }
        this
    }

    protected Object createServletContext() {
        if (IS_SERVLET_API_PRESENT) {
            def context = ClassUtils.forName('org.springframework.mock.web.MockServletContext').newInstance()
            Holders.setServletContext(context)
            return context
        }
    }

    protected ConfigurableApplicationContext createMainContext(Object servletContext) {
        ConfigurableApplicationContext context

        if (IS_SERVLET_API_PRESENT && servletContext != null) {
            AnnotationConfigServletWebApplicationContext annotationConfigServletWebApplicationContext =
                    (AnnotationConfigServletWebApplicationContext) ClassUtils.forName(
                            'org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext').newInstance()
            annotationConfigServletWebApplicationContext.setServletContext(servletContext)
            annotationConfigServletWebApplicationContext.register(DEFAULT_AUTO_CONFIGURATIONS)
            context = annotationConfigServletWebApplicationContext
        }
        else {
            AnnotationConfigApplicationContext annotationConfigApplicationContext =
                    (AnnotationConfigApplicationContext) ClassUtils.forName(
                            'org.springframework.context.annotation.AnnotationConfigApplicationContext').newInstance()
            annotationConfigApplicationContext.register(DEFAULT_AUTO_CONFIGURATIONS)
            context = annotationConfigApplicationContext
        }
        ConfigurableBeanFactory beanFactory = context.getBeanFactory()
        ((DefaultListableBeanFactory) beanFactory).setAllowBeanDefinitionOverriding(true)
        ((DefaultListableBeanFactory) beanFactory).setAllowCircularReferences(true)

        prepareContext(context)

        context.refresh()
        context.registerShutdownHook()

        context
    }

    protected void prepareContext(ConfigurableApplicationContext applicationContext) {
        registerGrailsAppPostProcessorBean(applicationContext.getBeanFactory())

        AnnotationConfigUtils.registerAnnotationConfigProcessors((BeanDefinitionRegistry) applicationContext.getBeanFactory())
        new ConfigDataApplicationContextInitializer().initialize(applicationContext)
    }

    void executeDoWithSpringCallback(GrailsApplication grailsApplication) {
        if (doWithSpring) {
            defineBeans(grailsApplication, doWithSpring)
        }
    }

    void defineBeans(Closure callable) {
        defineBeans(grailsApplication, callable)
    }

    void defineBeans(GrailsApplication grailsApplication, Closure callable) {
        def binding = new Binding()
        def bb = new BeanBuilder(null, null, grailsApplication.getClassLoader())
        binding.setVariable 'application', grailsApplication
        bb.setBinding binding
        bb.beans(callable)
        bb.registerBeans((BeanDefinitionRegistry) grailsApplication.getMainContext())
    }

    void registerBeans(GrailsApplication grailsApplication) {
        defineBeans(grailsApplication) { ->
            conversionService(ConversionServiceFactoryBean)

            xmlns context: 'http://www.springframework.org/schema/context'
            // adds AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor and others
            // see org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors method
            context.'annotation-config'()

            proxyHandler(DefaultProxyHandler)
            messageSource(StaticMessageSource)
            transactionManagerAwarePostProcessor(TransactionManagerPostProcessor)
            grailsPlaceholderConfigurer(GrailsPlaceholderConfigurer, '${', grailsApplication.config.toProperties())
            mapBasedSmartPropertyOverrideConfigurer(GrailsBeanPropertyOverrideConfigurer, grailsApplication)
        }
    }

    @CompileDynamic
    protected void registerGrailsAppPostProcessorBean(ConfigurableBeanFactory beanFactory) {
        GrailsApplication grailsApp

        Closure doWithSpringClosure = {
            registerBeans(grailsApp)
            executeDoWithSpringCallback(grailsApp)
        }

        Closure customizeGrailsApplicationClosure = { grailsApplication ->
            grailsApp = grailsApplication
            if (doWithConfig) {
                doWithConfig.call(grailsApplication.config)
                // reset flatConfig
                grailsApplication.configChanged()
            }
            Holders.config = grailsApplication.config
        }

        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues()
        constructorArgumentValues.addIndexedArgumentValue(0, doWithSpringClosure)
        constructorArgumentValues.addIndexedArgumentValue(1, includePlugins ?: DEFAULT_INCLUDED_PLUGINS)

        MutablePropertyValues values = new MutablePropertyValues()
        values.add('localOverride', localOverride)
        values.add('loadExternalBeans', loadExternalBeans)
        values.add('customizeGrailsApplicationClosure', customizeGrailsApplicationClosure)

        RootBeanDefinition beandef = new RootBeanDefinition(TestRuntimeGrailsApplicationPostProcessor, constructorArgumentValues, values)
        beandef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
        beanFactory.registerBeanDefinition('grailsApplicationPostProcessor', beandef)
    }

    static class TestRuntimeGrailsApplicationPostProcessor extends GrailsApplicationPostProcessor implements GroovyObject {

        Closure customizeGrailsApplicationClosure
        Set includedPlugins
        boolean localOverride = false

        TestRuntimeGrailsApplicationPostProcessor(Closure doWithSpringClosure, Set includedPlugins) {
            super()
            setGrailsApplicationLifeCycle([doWithSpring: { -> doWithSpringClosure }] as GrailsApplicationLifeCycle)
            loadExternalBeans = false
            reloadingEnabled = false
            this.includedPlugins = includedPlugins
        }

        @Override
        protected void customizePluginManager(GrailsPluginManager pluginManager) {
            super.customizePluginManager(pluginManager)
            pluginManager.pluginFilter = new IncludingPluginFilter(includedPlugins)
        }

        @Override
        protected void customizeGrailsApplication(GrailsApplication grailsApplication) {
            super.customizeGrailsApplication(grailsApplication)
            customizeGrailsApplicationClosure?.call(grailsApplication)
        }

        @Override
        void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            super.postProcessBeanDefinitionRegistry(registry)
            PropertySourcesPlaceholderConfigurer propertySourcePlaceholderConfigurer =
                    (PropertySourcesPlaceholderConfigurer) grailsApplication.mainContext.getBean('grailsPlaceholderConfigurer')
            propertySourcePlaceholderConfigurer.order = Ordered.HIGHEST_PRECEDENCE
            propertySourcePlaceholderConfigurer.setLocalOverride(localOverride)
        }

    }

}
