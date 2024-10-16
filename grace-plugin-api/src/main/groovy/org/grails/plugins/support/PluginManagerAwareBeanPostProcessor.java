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
package org.grails.plugins.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;

import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;

/**
 * Auto-injects beans that implement PluginManagerAware.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class PluginManagerAwareBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private GrailsPluginManager pluginManager;

    private BeanFactory beanFactory;

    public PluginManagerAwareBeanPostProcessor() {

    }

    public PluginManagerAwareBeanPostProcessor(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (this.pluginManager == null) {
            if (this.beanFactory.containsBean(GrailsPluginManager.BEAN_NAME)) {
                this.pluginManager = this.beanFactory.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
            }
        }

        if (this.pluginManager != null) {
            if (bean instanceof PluginManagerAware) {
                ((PluginManagerAware) bean).setPluginManager(this.pluginManager);
            }
        }

        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
