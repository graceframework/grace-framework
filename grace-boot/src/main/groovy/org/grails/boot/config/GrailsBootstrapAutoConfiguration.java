/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.boot.config;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import grails.boot.config.GrailsAutoConfiguration;
import grails.core.GrailsApplication;

import org.grails.boot.web.servlet.GrailsBootstrapClassRunner;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Grails Bootstrap Runner.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@AutoConfiguration(after = GrailsAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
public class GrailsBootstrapAutoConfiguration implements ServletContextAware {

    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Bean
    @ConditionalOnProperty(name = "grails.bootstrap.skip", havingValue = "false", matchIfMissing = true)
    public GrailsBootstrapClassRunner grailsBootstrapClassRunner(GrailsApplication grailsApplication) {
        GrailsBootstrapClassRunner bootstrapClassRunner = new GrailsBootstrapClassRunner();
        bootstrapClassRunner.setGrailsApplication(grailsApplication);
        bootstrapClassRunner.setServletContext(this.servletContext);
        return bootstrapClassRunner;
    }

}
