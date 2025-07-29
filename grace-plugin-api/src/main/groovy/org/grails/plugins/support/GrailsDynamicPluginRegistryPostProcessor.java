/*
 * Copyright 2021-2024 the original author or authors.
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
package org.grails.plugins.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;

import grails.plugins.GrailsPluginManager;

/**
 * Use {@link BeanDefinitionRegistryPostProcessor} to load dynamic modules in all Plugins.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsDynamicPluginRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, Ordered {

    private static final Log logger = LogFactory.getLog(GrailsDynamicPluginRegistryPostProcessor.class);

    protected ApplicationContext applicationContext;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsSingleton(GrailsPluginManager.BEAN_NAME)) {
            return;
        }

        GrailsPluginManager pluginManager = beanFactory.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
        pluginManager.doDynamicModules();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return 10;
    }

}
