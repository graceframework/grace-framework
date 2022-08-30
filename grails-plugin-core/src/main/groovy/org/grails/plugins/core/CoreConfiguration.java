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
package org.grails.plugins.core;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import grails.config.ConfigProperties;
import grails.core.GrailsApplication;

/**
 * Core Auto-Configuration.
 *
 * @author graemerocher
 * @author Michael Yan
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder
public class CoreConfiguration {

    private final GrailsApplication grailsApplication;

    public CoreConfiguration(ObjectProvider<GrailsApplication> grailsApplication) {
        this.grailsApplication = grailsApplication.getIfAvailable();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ClassLoader classLoader() {
        return this.grailsApplication.getClassLoader();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ConfigProperties grailsConfigProperties() {
        return new ConfigProperties(this.grailsApplication.getConfig());
    }

}
