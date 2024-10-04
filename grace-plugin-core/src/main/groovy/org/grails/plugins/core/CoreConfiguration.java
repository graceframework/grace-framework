/*
 * Copyright 2004-2024 the original author or authors.
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

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import grails.config.ConfigProperties;
import grails.core.GrailsApplication;
import grails.core.support.proxy.DefaultProxyHandler;
import grails.core.support.proxy.ProxyHandler;
import grails.util.BuildSettings;
import org.grails.core.io.DefaultResourceLocator;

/**
 * Core Auto-Configuration.
 *
 * @author graemerocher
 * @author Michael Yan
 * @since 4.0
 */
@AutoConfiguration
@AutoConfigureOrder(300)
public class CoreConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ClassLoader classLoader(ObjectProvider<GrailsApplication> grailsApplication) {
        return grailsApplication.getIfAvailable().getClassLoader();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ConfigProperties grailsConfigProperties(ObjectProvider<GrailsApplication> grailsApplication) {
        return new ConfigProperties(grailsApplication.getIfAvailable().getConfig());
    }

    @Bean
    @Order(0)
    @ConditionalOnMissingBean
    public DefaultResourceLocator grailsResourceLocator() throws IOException {
        DefaultResourceLocator defaultResourceLocator = new DefaultResourceLocator();
        defaultResourceLocator.setSearchLocations(List.of(BuildSettings.BASE_DIR.getCanonicalPath()));

        return defaultResourceLocator;
    }

    @Bean
    @Order(100)
    @ConditionalOnMissingBean
    public ProxyHandler proxyHandler() {
        return new DefaultProxyHandler();
    }

}
