/*
 * Copyright 2022-2023 the original author or authors.
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
package org.grails.plugins.web.controllers;

import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;

import org.grails.web.errors.GrailsExceptionResolver;
import org.grails.web.filters.HiddenHttpMethodFilter;
import org.grails.web.filters.OrderedHiddenHttpMethodFilter;
import org.grails.web.servlet.mvc.GrailsDispatcherServlet;
import org.grails.web.servlet.mvc.GrailsWebRequestFilter;
import org.grails.web.servlet.mvc.ParameterCreationListener;
import org.grails.web.servlet.mvc.TokenResponseActionResultTransformer;
import org.grails.web.servlet.view.CompositeViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Grails Controllers Plugin.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@AutoConfiguration(before = { WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ Servlet.class })
public class ControllersPluginConfiguration {

    @Bean
    public TokenResponseActionResultTransformer tokenResponseActionResultTransformer() {
        return new TokenResponseActionResultTransformer();
    }

    @Bean
    public FilterRegistrationBean<GrailsWebRequestFilter> grailsWebRequestFilter(
            ObjectProvider<ParameterCreationListener> parameterCreationListenerProvider) {
        GrailsWebRequestFilter filter = new GrailsWebRequestFilter();
        filter.setParameterCreationListeners(parameterCreationListenerProvider.stream().collect(Collectors.toList()));
        FilterRegistrationBean<GrailsWebRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE);
        registration.setOrder(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 30);
        return registration;
    }

    @Bean
    public GrailsExceptionResolver exceptionHandler() {
        GrailsExceptionResolver exceptionResolver = new GrailsExceptionResolver();
        Properties exceptionMappings = new Properties();
        exceptionMappings.put("java.lang.Exception", "/error");
        exceptionResolver.setExceptionMappings(exceptionMappings);
        return exceptionResolver;
    }

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    public GrailsDispatcherServlet dispatcherServlet() {
        return new GrailsDispatcherServlet();
    }

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
    public DispatcherServletRegistrationBean dispatcherServletRegistration(ObjectProvider<GrailsApplication> grailsApplicationProvider,
            GrailsDispatcherServlet dispatcherServlet, ObjectProvider<MultipartConfigElement> multipartConfig) {
        GrailsApplication grailsApplication = grailsApplicationProvider.getIfAvailable();
        Config config = grailsApplication.getConfig();
        boolean isTomcat = ClassUtils.isPresent("org.apache.catalina.startup.Tomcat", grailsApplication.getClassLoader());
        String grailsServletPath = config.getProperty(Settings.WEB_SERVLET_PATH,
                isTomcat ? Settings.DEFAULT_TOMCAT_SERVLET_PATH : Settings.DEFAULT_WEB_SERVLET_PATH);

        DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(dispatcherServlet, grailsServletPath);
        registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
        registration.setLoadOnStartup(2);
        registration.setAsyncSupported(true);
        multipartConfig.ifAvailable(registration::setMultipartConfig);
        return registration;
    }

    @Bean
    public CompositeViewResolver compositeViewResolver(ObjectProvider<ViewResolver> viewResolverProviders) {
        CompositeViewResolver compositeViewResolver = new CompositeViewResolver();
        compositeViewResolver.setViewResolvers(viewResolverProviders.orderedStream().collect(Collectors.toList()));
        return compositeViewResolver;
    }

    @Bean
    public WebMvcConfigurer webMvcConfig(ObjectProvider<GrailsApplication> grailsApplicationProvider) {
        GrailsApplication grailsApplication = grailsApplicationProvider.getIfAvailable();
        Config config = grailsApplication.getConfig();
        int resourcesCachePeriod = config.getProperty(Settings.RESOURCES_CACHE_PERIOD, Integer.class, 0);
        boolean resourcesEnabled = config.getProperty(Settings.RESOURCES_ENABLED, Boolean.class, true);
        String resourcesPattern = config.getProperty(Settings.RESOURCES_PATTERN, String.class, Settings.DEFAULT_RESOURCE_PATTERN);

        GrailsWebMvcConfigurer webMvcConfigurer = new GrailsWebMvcConfigurer(resourcesCachePeriod, resourcesEnabled, resourcesPattern);
        return webMvcConfigurer;
    }

    @Bean
    @ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
    @ConditionalOnProperty(prefix = "grails.web.hiddenmethod.filter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new OrderedHiddenHttpMethodFilter();
    }

    static class GrailsWebMvcConfigurer implements WebMvcConfigurer {

        private static final String[] SERVLET_RESOURCE_LOCATIONS = new String[] { "/" };

        private static final String[] CLASSPATH_RESOURCE_LOCATIONS = new String[] {
                "classpath:/META-INF/resources/", "classpath:/resources/",
                "classpath:/static/", "classpath:/public/" };

        private static final String[] RESOURCE_LOCATIONS;

        static {
            RESOURCE_LOCATIONS = new String[CLASSPATH_RESOURCE_LOCATIONS.length + SERVLET_RESOURCE_LOCATIONS.length];
            System.arraycopy(SERVLET_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS, 0,
                    SERVLET_RESOURCE_LOCATIONS.length);
            System.arraycopy(CLASSPATH_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS,
                    SERVLET_RESOURCE_LOCATIONS.length, CLASSPATH_RESOURCE_LOCATIONS.length);
        }

        boolean addMappings = true;

        Integer cachePeriod;

        String resourcesPattern = "/static/**";

        GrailsWebMvcConfigurer(Integer cachePeriod, boolean addMappings, String resourcesPattern) {
            this.addMappings = addMappings;
            this.cachePeriod = cachePeriod;
            this.resourcesPattern = resourcesPattern;
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            if (!this.addMappings) {
                return;
            }

            if (!registry.hasMappingForPattern("/webjars/**")) {
                registry.addResourceHandler("/webjars/**")
                        .addResourceLocations("classpath:/META-INF/resources/webjars/")
                        .setCachePeriod(this.cachePeriod);
            }
            if (!registry.hasMappingForPattern(this.resourcesPattern)) {
                registry.addResourceHandler(this.resourcesPattern)
                        .addResourceLocations(RESOURCE_LOCATIONS)
                        .setCachePeriod(this.cachePeriod);
            }
        }

    }

}
