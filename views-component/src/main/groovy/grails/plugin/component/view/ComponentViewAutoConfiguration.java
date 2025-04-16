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
package grails.plugin.component.view;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import grails.plugin.component.view.mvc.ComponentViewResolver;
import grails.views.mvc.GenericGroovyTemplateViewResolver;
import grails.views.resolve.PluginAwareTemplateResolver;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for Component View
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@EnableConfigurationProperties(ComponentViewConfiguration.class)
public class ComponentViewAutoConfiguration {

    private final ComponentViewConfiguration componentViewConfiguration;
    private final ApplicationContext applicationContext;

    public ComponentViewAutoConfiguration(ComponentViewConfiguration componentViewConfiguration, ApplicationContext applicationContext) {
        this.componentViewConfiguration = componentViewConfiguration;
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public ComponentViewTemplateEngine componentTemplateEngine() {
        return new ComponentViewTemplateEngine(this.componentViewConfiguration, this.applicationContext.getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean
    public ComponentViewResolver smartComponentViewResolver(ComponentViewTemplateEngine componentTemplateEngine) {
        ComponentViewResolver smartComponentViewResolver = new ComponentViewResolver(componentTemplateEngine);
        smartComponentViewResolver.setTemplateResolver(new PluginAwareTemplateResolver(this.componentViewConfiguration));
        return smartComponentViewResolver;
    }

    @Bean
    @ConditionalOnMissingBean(name = "componentViewResolver")
    public GenericGroovyTemplateViewResolver componentViewResolver(ComponentViewResolver smartComponentViewResolver) {
        return new GenericGroovyTemplateViewResolver(smartComponentViewResolver);
    }

}
