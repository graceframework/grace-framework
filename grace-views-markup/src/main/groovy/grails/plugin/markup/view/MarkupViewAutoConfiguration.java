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
package grails.plugin.markup.view;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import grails.plugin.markup.view.mvc.MarkupViewResolver;
import grails.views.mvc.GenericGroovyTemplateViewResolver;
import grails.views.resolve.PluginAwareTemplateResolver;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for Markup View
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@EnableConfigurationProperties(MarkupViewConfiguration.class)
public class MarkupViewAutoConfiguration {

    private final MarkupViewConfiguration markupViewConfiguration;
    private final ApplicationContext applicationContext;

    public MarkupViewAutoConfiguration(MarkupViewConfiguration markupViewConfiguration, ApplicationContext applicationContext) {
        this.markupViewConfiguration = markupViewConfiguration;
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public MarkupViewTemplateEngine markupTemplateEngine() {
        return new MarkupViewTemplateEngine(this.markupViewConfiguration, this.applicationContext.getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean
    public MarkupViewResolver smartMarkupViewResolver(MarkupViewTemplateEngine markupTemplateEngine) {
        MarkupViewResolver smartMarkupViewResolver = new MarkupViewResolver(markupTemplateEngine);
        smartMarkupViewResolver.setTemplateResolver(new PluginAwareTemplateResolver(this.markupViewConfiguration));
        return smartMarkupViewResolver;
    }

    @Bean
    @ConditionalOnMissingBean(name = "markupViewResolver")
    public GenericGroovyTemplateViewResolver markupViewResolver(MarkupViewResolver smartMarkupViewResolver) {
        return new GenericGroovyTemplateViewResolver(smartMarkupViewResolver);
    }

}
