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
package grails.plugin.json.view;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import grails.plugin.json.view.api.jsonapi.DefaultJsonApiIdRenderer;
import grails.plugin.json.view.api.jsonapi.JsonApiIdRenderStrategy;
import grails.plugin.json.view.mvc.JsonViewResolver;
import grails.views.mvc.GenericGroovyTemplateViewResolver;
import grails.views.resolve.PluginAwareTemplateResolver;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for Json View
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@EnableConfigurationProperties(JsonViewConfiguration.class)
public class JsonViewAutoConfiguration {

    private final JsonViewConfiguration jsonViewConfiguration;
    private final ApplicationContext applicationContext;

    public JsonViewAutoConfiguration(JsonViewConfiguration jsonViewConfiguration, ApplicationContext applicationContext) {
        this.jsonViewConfiguration = jsonViewConfiguration;
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonApiIdRenderStrategy jsonApiIdRenderStrategy() {
        return new DefaultJsonApiIdRenderer();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonViewTemplateEngine jsonTemplateEngine(JsonApiIdRenderStrategy jsonApiIdRenderStrategy) {
        JsonViewTemplateEngine jsonViewTemplateEngine = new JsonViewTemplateEngine(this.jsonViewConfiguration, this.applicationContext.getClassLoader());
        jsonViewTemplateEngine.setJsonApiIdRenderStrategy(jsonApiIdRenderStrategy);
        return jsonViewTemplateEngine;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonViewResolver jsonSmartViewResolver(JsonViewTemplateEngine jsonTemplateEngine) {
        JsonViewResolver jsonSmartViewResolver = new JsonViewResolver(jsonTemplateEngine);
        jsonSmartViewResolver.setTemplateResolver(new PluginAwareTemplateResolver(this.jsonViewConfiguration));
        return jsonSmartViewResolver;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonViewResolver")
    public GenericGroovyTemplateViewResolver jsonViewResolver(JsonViewResolver jsonSmartViewResolver) {
        return new GenericGroovyTemplateViewResolver(jsonSmartViewResolver);
    }

}
