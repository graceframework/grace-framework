/*
 * Copyright 2024-2026 the original author or authors.
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
package org.grails.plugins.events;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import grails.events.bus.EventBus;

import org.grails.events.bus.spring.EventBusFactoryBean;
import org.grails.events.gorm.GormDispatcherRegistrar;
import org.grails.events.spring.SpringEventTranslator;
import org.grails.plugins.web.async.ControllersAsyncAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Events.
 *
 * @author Michael Yan
 * @since 2023.1
 */
@AutoConfiguration(after = ControllersAsyncAutoConfiguration.class)
public class EventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus() throws Exception {
        EventBusFactoryBean factoryBean = new EventBusFactoryBean();
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public GormDispatcherRegistrar gormDispatchEventRegistrar(EventBus eventBus) {
        return new GormDispatcherRegistrar(eventBus);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "grails.events.spring", havingValue = "true")
    public SpringEventTranslator springEventTranslator(EventBus eventBus) {
        return new SpringEventTranslator(eventBus);
    }

}
