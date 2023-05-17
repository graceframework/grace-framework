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
package org.grails.plugins.web;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import grails.core.GrailsApplication;
import grails.util.Environment;

import org.grails.web.pages.FilteringCodecsByContentTypeSettings;
import org.grails.web.pages.GroovyPagesServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Groovy Pages
 *
 * @author Michael Yan
 * @since 2022.2.3
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE - 1000)
public class GroovyPagesAutoConfiguration {

    @Bean
    public FilteringCodecsByContentTypeSettings filteringCodecsByContentTypeSettings(ObjectProvider<GrailsApplication> grailsApplication) {

        return new FilteringCodecsByContentTypeSettings(grailsApplication.getIfAvailable());
    }

    @Bean
    public DefaultGrailsTagDateHelper grailsTagDateHelper() {
        return new DefaultGrailsTagDateHelper();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ServletRegistrationBean<GroovyPagesServlet> groovyPagesServlet() {
        ServletRegistrationBean<GroovyPagesServlet> servletRegistration = new ServletRegistrationBean<>();
        servletRegistration.setServlet(new GroovyPagesServlet());
        servletRegistration.setUrlMappings(List.of("*.gsp"));
        if (Environment.isDevelopmentMode()) {
            servletRegistration.addInitParameter("showSource", "1");
        }
        return servletRegistration;
    }

}
