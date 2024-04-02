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
package org.grails.plugins.web.mapping;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.plugins.GrailsPluginManager;
import grails.util.Environment;
import grails.web.CamelCaseUrlConverter;
import grails.web.HyphenatedUrlConverter;
import grails.web.UrlConverter;
import grails.web.mapping.LinkGenerator;
import grails.web.mapping.UrlMappings;
import grails.web.mapping.UrlMappingsHolder;
import grails.web.mapping.cors.GrailsCorsConfiguration;
import grails.web.mapping.cors.GrailsCorsFilter;

import org.grails.plugins.web.controllers.ControllersPluginConfiguration;
import org.grails.web.mapping.CachingLinkGenerator;
import org.grails.web.mapping.DefaultLinkGenerator;
import org.grails.web.mapping.UrlMappingsHolderFactoryBean;
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping;
import org.grails.web.mapping.mvc.UrlMappingsInfoHandlerAdapter;
import org.grails.web.mapping.servlet.UrlMappingsErrorPageCustomizer;
import org.grails.web.servlet.mvc.ActionResultTransformer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Grails UrlMappings Plugin.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@AutoConfiguration(after = ControllersPluginConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({ UrlMappingsProperties.class, GrailsCorsConfiguration.class })
public class UrlMappingsPluginConfiguration {

    @Bean(name = UrlConverter.BEAN_NAME)
    @ConditionalOnMissingBean(name = UrlConverter.BEAN_NAME)
    @ConditionalOnProperty(name = "grails.web.url.converter", havingValue = "camelCase", matchIfMissing = true)
    public UrlConverter camelCaseUrlConverter() {
        return new CamelCaseUrlConverter();
    }

    @Bean(name = UrlConverter.BEAN_NAME)
    @ConditionalOnMissingBean(name = UrlConverter.BEAN_NAME)
    @ConditionalOnProperty(name = "grails.web.url.converter", havingValue = "hyphenated")
    public UrlConverter hyphenatedUrlConverter() {
        return new HyphenatedUrlConverter();
    }

    @Bean
    public UrlMappingsHandlerMapping urlMappingsHandlerMapping(ObjectProvider<GrailsApplication> grailsApplicationProvider,
            ObjectProvider<UrlMappingsHolder> urlMappingsHolderProvider,
            GrailsCorsConfiguration grailsCorsConfiguration) {

        Config config = grailsApplicationProvider.getIfAvailable().getConfig();
        boolean corsFilterEnabled = config.getProperty(Settings.SETTING_CORS_FILTER, Boolean.class, true);

        UrlMappingsHandlerMapping handlerMapping = new UrlMappingsHandlerMapping(urlMappingsHolderProvider.getIfAvailable());

        if (!corsFilterEnabled) {
            handlerMapping.setGrailsCorsConfiguration(grailsCorsConfiguration);
        }

        return handlerMapping;
    }

    @Bean
    public UrlMappingsInfoHandlerAdapter urlMappingsInfoHandlerAdapter(
            ObjectProvider<ActionResultTransformer> actionResultTransformerProvider,
            LinkGenerator grailsLinkGenerator) {

        List<ActionResultTransformer> actionResultTransformers = actionResultTransformerProvider
                .orderedStream().collect(Collectors.toList());
        UrlMappingsInfoHandlerAdapter handlerAdapter = new UrlMappingsInfoHandlerAdapter();
        handlerAdapter.setActionResultTransformers(actionResultTransformers);
        handlerAdapter.setLinkGenerator(grailsLinkGenerator);

        return handlerAdapter;
    }

    @Bean
    public UrlMappingsHolderFactoryBean grailsUrlMappingsHolder(
            ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<GrailsPluginManager> pluginManager,
            ObjectProvider<UrlConverter> urlConverterProvider) {

        UrlMappingsHolderFactoryBean factoryBean = new UrlMappingsHolderFactoryBean();
        factoryBean.setGrailsApplication(grailsApplication.getIfAvailable());
        factoryBean.setPluginManager(pluginManager.getIfAvailable());
        factoryBean.setUrlConverter(urlConverterProvider.getIfAvailable());

        return factoryBean;
    }

    @Bean({"linkGenerator", "grailsLinkGenerator"})
    public LinkGenerator grailsLinkGenerator(ObjectProvider<GrailsApplication> grailsApplicationProvider,
            ObjectProvider<UrlMappingsHolder> grailsUrlMappingsHolder) {
        Config config = grailsApplicationProvider.getIfAvailable().getConfig();
        boolean isReloadEnabled = Environment.isDevelopmentMode() || Environment.getCurrent().isReloadEnabled();
        boolean cacheUrls = config.getProperty(Settings.WEB_LINK_GENERATOR_USE_CACHE, Boolean.class, !isReloadEnabled);
        String serverURL = config.getProperty(Settings.SERVER_URL);

        DefaultLinkGenerator linkGenerator = cacheUrls ? new CachingLinkGenerator(serverURL) : new DefaultLinkGenerator(serverURL);
        linkGenerator.setUrlMappingsHolder(grailsUrlMappingsHolder.getIfAvailable());
        return linkGenerator;
    }

    @Bean
    public UrlMappingsErrorPageCustomizer urlMappingsErrorPageCustomizer(ObjectProvider<UrlMappings> urlMappingsProvider) {
        UrlMappingsErrorPageCustomizer errorPageCustomizer = new UrlMappingsErrorPageCustomizer();
        errorPageCustomizer.setUrlMappings(urlMappingsProvider.getIfAvailable());
        return errorPageCustomizer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "grails.cors", name = "filter.enabled", havingValue = "true", matchIfMissing = true)
    public GrailsCorsFilter grailsCorsFilter(GrailsCorsConfiguration grailsCorsConfiguration) {
        return new GrailsCorsFilter(grailsCorsConfiguration);
    }

}
