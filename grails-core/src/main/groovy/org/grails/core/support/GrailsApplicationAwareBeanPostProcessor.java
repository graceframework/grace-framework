/*
 * Copyright 2003-2022 the original author or authors.
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
package org.grails.core.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import grails.core.GrailsApplication;
import grails.core.support.GrailsApplicationAware;
import grails.core.support.GrailsConfigurationAware;

/**
 * Implementation of {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * that recognizes {@link grails.core.support.GrailsApplicationAware}
 * and injects and instance of {@link GrailsApplication}.
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class GrailsApplicationAwareBeanPostProcessor implements BeanPostProcessor {

    private final GrailsApplication grailsApplication;

    public GrailsApplicationAwareBeanPostProcessor(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        processAwareInterfaces(this.grailsApplication, bean);
        return bean;
    }

    public static void processAwareInterfaces(GrailsApplication grailsApplication, Object bean) {
        if (bean instanceof GrailsApplicationAware) {
            ((GrailsApplicationAware) bean).setGrailsApplication(grailsApplication);
        }
        if (bean instanceof GrailsConfigurationAware) {
            ((GrailsConfigurationAware) bean).setConfiguration(grailsApplication.getConfig());
        }
    }

}
