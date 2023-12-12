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
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.util.Environment;

import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource;
import org.grails.web.i18n.ParamsAwareLocaleChangeInterceptor;

/**
 * Grails locale configuration.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(MessageSourceAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 8)
public class GrailsI18nPluginConfiguration {

    private final GrailsApplication grailsApplication;

    private final WebProperties webProperties;

    public GrailsI18nPluginConfiguration(ObjectProvider<GrailsApplication> grailsApplication, WebProperties webProperties) {
        this.grailsApplication = grailsApplication.getIfAvailable();
        this.webProperties = webProperties;
    }

    @Bean
    @ConfigurationProperties(prefix = "grails.i18n")
    public MessageSourceProperties grailsMessageSourceProperties() {
        return new MessageSourceProperties();
    }

    @Bean
    @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
    public MessageSource messageSource(MessageSourceProperties properties) {
        PluginAwareResourceBundleMessageSource messageSource = new PluginAwareResourceBundleMessageSource();
        Config config = this.grailsApplication.getConfig();

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

    @Bean
    @ConditionalOnMissingBean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        ParamsAwareLocaleChangeInterceptor localeChangeInterceptor = new ParamsAwareLocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @Bean
    @ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver() {
        if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
            return new FixedLocaleResolver(this.webProperties.getLocale());
        }
        else if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.ACCEPT_HEADER) {
            AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
            localeResolver.setDefaultLocale(this.webProperties.getLocale());
            return localeResolver;
        }
        return new SessionLocaleResolver();
    }

}
