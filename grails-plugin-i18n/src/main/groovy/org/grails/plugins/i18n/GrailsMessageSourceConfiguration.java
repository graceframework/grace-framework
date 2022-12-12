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
package org.grails.plugins.i18n;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.util.Environment;

import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource;

/**
 * Grails {@link MessageSource} configuration.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(MessageSourceAutoConfiguration.class)
public class GrailsMessageSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "grails.i18n")
    public MessageSourceProperties grailsMessageSourceProperties() {
        return new MessageSourceProperties();
    }

    @Bean
    @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
    public MessageSource messageSource(ObjectProvider<GrailsApplication> grailsApplication, MessageSourceProperties properties) {
        PluginAwareResourceBundleMessageSource messageSource = new PluginAwareResourceBundleMessageSource();
        Config config = grailsApplication.getIfAvailable().getConfig();

        String encoding;
        if (properties.getEncoding() != null) {
            encoding = properties.getEncoding().name();
        }
        else {
            encoding = config.getProperty(Settings.GSP_VIEW_ENCODING, "UTF-8");
        }

        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        int cacheSeconds = config.getProperty(Settings.I18N_CACHE_SECONDS, Integer.class, 5);
        int fileCacheSeconds = config.getProperty(Settings.I18N_FILE_CACHE_SECONDS, Integer.class, 5);

        messageSource.setDefaultEncoding(encoding);
        messageSource.setFallbackToSystemLocale(false);
        if (Environment.getCurrent().isReloadEnabled() || gspEnableReload) {
            messageSource.setCacheSeconds(cacheSeconds);
            messageSource.setFileCacheSeconds(fileCacheSeconds);
        }

        return messageSource;
    }

}
