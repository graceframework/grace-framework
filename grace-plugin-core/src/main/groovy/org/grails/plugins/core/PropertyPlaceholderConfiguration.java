/*
 * Copyright 2021-2023 the original author or authors.
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
package org.grails.plugins.core;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;

import org.grails.spring.context.support.GrailsBeanPropertyOverrideConfigurer;
import org.grails.spring.context.support.GrailsPlaceholderConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link GrailsPlaceholderConfigurer}.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@AutoConfiguration(before = PropertyPlaceholderAutoConfiguration.class)
@AutoConfigureOrder(10010)
public class PropertyPlaceholderConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
    public GrailsPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ObjectProvider<GrailsApplication> grailsApplication) {
        Config config = grailsApplication.getIfAvailable().getConfig();
        String placeHolderPrefix = config.getProperty(Settings.SPRING_PLACEHOLDER_PREFIX, "${");
        GrailsPlaceholderConfigurer grailsPlaceholderConfigurer = new GrailsPlaceholderConfigurer();
        grailsPlaceholderConfigurer.setPlaceholderPrefix(placeHolderPrefix);
        return grailsPlaceholderConfigurer;
    }

    @Bean
    public GrailsBeanPropertyOverrideConfigurer beanPropertyOverrideConfigurer(ObjectProvider<GrailsApplication> grailsApplication) {
        return new GrailsBeanPropertyOverrideConfigurer(grailsApplication.getIfAvailable());
    }

}
