/*
 * Copyright 2024 the original author or authors.
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
package org.grails.plugins.converters;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import grails.core.GrailsApplication;
import grails.core.support.proxy.ProxyHandler;
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer;

/**
 * {@link EnableAutoConfiguration Auto-configure} for Converters
 *
 * @author Michael Yan
 * @since 2023.1
 */
@AutoConfiguration
@Import({JsonConvertersConfiguration.class, XmlConvertersConfiguration.class})
public class ConvertersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConvertersConfigurationInitializer convertersConfigurationInitializer(
            ObjectProvider<ApplicationContext> applicationContext,
            ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<ProxyHandler> proxyHandler) {
        ConvertersConfigurationInitializer convertersInitializer = new ConvertersConfigurationInitializer();
        convertersInitializer.setApplicationContext(applicationContext.getObject());
        convertersInitializer.setGrailsApplication(grailsApplication.getObject());
        convertersInitializer.setProxyHandler(proxyHandler.getObject());
        return convertersInitializer;
    }

}
