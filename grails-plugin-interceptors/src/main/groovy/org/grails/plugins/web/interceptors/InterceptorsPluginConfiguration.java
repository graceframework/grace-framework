/*
 * Copyright 2021-2022 the original author or authors.
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
package org.grails.plugins.web.interceptors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;

import grails.artefact.Interceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Grails Interceptors Plugin.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InterceptorsPluginConfiguration {

    @Bean
    public MappedInterceptor grailsInterceptorMappedInterceptor(
            GrailsInterceptorHandlerInterceptorAdapter grailsInterceptorHandlerInterceptorAdapter) {

        return new MappedInterceptor(new String[] { "/**" }, grailsInterceptorHandlerInterceptorAdapter);
    }

    @Bean
    public GrailsInterceptorHandlerInterceptorAdapter grailsInterceptorHandlerInterceptorAdapter(
            ObjectProvider<Interceptor> interceptorProvider) {

        Interceptor[] interceptors = interceptorProvider.orderedStream().toArray(Interceptor[]::new);
        GrailsInterceptorHandlerInterceptorAdapter handlerInterceptorAdapter = new GrailsInterceptorHandlerInterceptorAdapter();
        handlerInterceptorAdapter.setInterceptors(interceptors);
        return handlerInterceptorAdapter;
    }

}
