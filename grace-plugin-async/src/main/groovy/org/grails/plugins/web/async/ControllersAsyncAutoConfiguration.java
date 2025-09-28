/*
 * Copyright 2024-2025 the original author or authors.
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
package org.grails.plugins.web.async;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

import grails.async.PromiseFactory;

import org.grails.plugins.web.async.mvc.AsyncActionResultTransformer;
import org.grails.plugins.web.async.spring.PromiseFactoryBean;
import org.grails.plugins.web.controllers.ControllersPluginConfiguration;
import org.grails.web.errors.GrailsExceptionResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Async Controllers.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@AutoConfiguration(after = ControllersPluginConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ControllersAsyncAutoConfiguration {

    @Bean
    public AsyncActionResultTransformer asyncPromiseResponseActionResultTransformer(ObjectProvider<GrailsExceptionResolver> exceptionResolver) {
        AsyncActionResultTransformer actionResultTransformer = new AsyncActionResultTransformer();
        exceptionResolver.ifAvailable(actionResultTransformer::setExceptionResolver);
        return new AsyncActionResultTransformer();
    }

    @Bean
    @ConditionalOnMissingBean
    public PromiseFactory grailsPromiseFactory() throws Exception {
        return new PromiseFactoryBean().getObject();
    }

}
