/*
 * Copyright 2009-2022 the original author or authors.
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
package org.grails.spring.context.support;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

import grails.core.GrailsApplication;

/**
 * Property resource configurer that overrides bean property values in an application
 * context definition. It <i>pushes</i> values from a properties file or 'application.groovy' into bean definitions.
 *
 * <p>You define a beans block with the names of beans and their values:
 * <pre class="code">
 * beans {
 *     bookService {
 *         webServiceURL = "http://www.amazon.com"
 *     }
 * }</pre>
 *
 * <p>The general format is:
 * <pre class="code">
 * <&lt;bean name&gt;>.<&lt;property name&gt;> = <&lt;value&gt;> </pre>
 * The same configuration in a Java properties file would be:
 * <pre class="code">
 * beans.bookService.webServiceURL=http://www.amazon.com
 * </pre>
 *
 * @author Luke Daley
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2022.0.x
 * @see PropertyOverrideConfigurer
 */
public class GrailsBeanPropertyOverrideConfigurer implements BeanFactoryPostProcessor, PriorityOrdered {

    private final GrailsApplication grailsApplication;

    private int order = Ordered.LOWEST_PRECEDENCE;

    public GrailsBeanPropertyOverrideConfigurer(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    /**
     * Set the order value of this object for sorting purposes.
     * @see PriorityOrdered
     */
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        Map<String, Object> beans = getBeansConfig();
        if (beans == null) {
            return;
        }
        for (String beanName : beans.keySet()) {
            Object beanProperties = beans.get(beanName);
            if (beanProperties instanceof Map) {
                Map beanPropertiesMap = (Map) beanProperties;
                for (Object beanPropertyName : beanPropertiesMap.keySet()) {
                    String beanPropertyValue = beanPropertiesMap.get(beanPropertyName).toString();
                    applyPropertyValue(factory, beanName, beanPropertyName.toString(), beanPropertyValue);
                }
            }
            else {
                throw new IllegalArgumentException("Entry in bean config for bean '$beanName' must be a Map");
            }
        }
    }

    protected Map<String, Object> getBeansConfig() {
        return this.grailsApplication.getConfig().getProperty("beans", Map.class);
    }

    protected void applyPropertyValue(
            ConfigurableListableBeanFactory factory, String beanName, String property, String value) {

        BeanDefinition bd = getTargetBeanDefinition(factory, beanName);
        BeanDefinition bdToUse = bd;
        while (bd != null) {
            bdToUse = bd;
            bd = bd.getOriginatingBeanDefinition();
        }
        PropertyValue pv = new PropertyValue(property, value);
        pv.setOptional(true);
        if (bdToUse != null) {
            bdToUse.getPropertyValues().addPropertyValue(pv);
        }
    }

    protected BeanDefinition getTargetBeanDefinition(ConfigurableListableBeanFactory factory, String beanName) {
        if (factory.containsBeanDefinition(beanName)) {
            return getTargetBeanDefinition(factory, beanName, factory.getBeanDefinition(beanName));
        }
        else {
            return null;
        }
    }

    protected BeanDefinition getTargetBeanDefinition(ConfigurableListableBeanFactory factory,
            String beanName, BeanDefinition beanDefinition) {

        if (beanDefinition.getFactoryBeanName() != null) {
            return beanDefinition;
        }
        else {
            try {
                Class<?> factoryBeanClass = this.grailsApplication.getClassLoader().loadClass(beanDefinition.getBeanClassName());
                return getTargetBeanDefinition(factory, beanName, beanDefinition, factoryBeanClass);
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    protected BeanDefinition getTargetBeanDefinition(ConfigurableListableBeanFactory factory, String beanName,
            BeanDefinition beanDefinition, Class<?> beanClass) {

        if (FactoryBean.class.isAssignableFrom(beanClass)) {
            return getTargetBeanDefinitionForFactoryBean(factory, beanName, beanDefinition, beanClass);
        }
        else {
            return beanDefinition;
        }
    }

    protected BeanDefinition getTargetBeanDefinitionForFactoryBean(ConfigurableListableBeanFactory factory,
            String beanName, BeanDefinition beanDefinition, Class<?> beanClass) {

        if (TransactionProxyFactoryBean.class.isAssignableFrom(beanClass)) {
            return getTargetBeanDefinition(factory, beanName,
                    (BeanDefinition) Objects.requireNonNull(beanDefinition.getPropertyValues().getPropertyValue("target").getValue()));
        }
        else {
            return beanDefinition;
        }
    }

}
