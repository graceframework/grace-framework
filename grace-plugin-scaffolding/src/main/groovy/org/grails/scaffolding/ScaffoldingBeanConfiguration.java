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
package org.grails.scaffolding;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import grails.web.mapping.LinkGenerator;

import org.grails.datastore.mapping.model.MappingContext;
import org.grails.scaffolding.markup.ContextMarkupRenderer;
import org.grails.scaffolding.markup.ContextMarkupRendererImpl;
import org.grails.scaffolding.markup.DomainMarkupRenderer;
import org.grails.scaffolding.markup.DomainMarkupRendererImpl;
import org.grails.scaffolding.markup.PropertyMarkupRenderer;
import org.grails.scaffolding.markup.PropertyMarkupRendererImpl;
import org.grails.scaffolding.model.DomainModelService;
import org.grails.scaffolding.model.DomainModelServiceImpl;
import org.grails.scaffolding.model.property.DomainPropertyFactory;
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl;
import org.grails.scaffolding.registry.DomainInputRendererRegistry;
import org.grails.scaffolding.registry.DomainOutputRendererRegistry;
import org.grails.scaffolding.registry.DomainRendererRegisterer;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for Scaffolding
 *
 * @author Michael Yan
 * @since 6.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class ScaffoldingBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ContextMarkupRenderer contextMarkupRenderer(ObjectProvider<MessageSource> messageSources) {
        return new ContextMarkupRendererImpl(messageSources.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainMarkupRenderer domainMarkupRenderer(DomainModelService domainModelService, PropertyMarkupRenderer propertyMarkupRenderer,
            ContextMarkupRenderer contextMarkupRenderer) {
        return new DomainMarkupRendererImpl(domainModelService, propertyMarkupRenderer, contextMarkupRenderer);
    }

    @Bean
    @ConditionalOnMissingBean
    public PropertyMarkupRenderer propertyMarkupRenderer(DomainInputRendererRegistry domainInputRendererRegistry,
            DomainOutputRendererRegistry domainOutputRendererRegistry) {
        return new PropertyMarkupRendererImpl(domainInputRendererRegistry, domainOutputRendererRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainPropertyFactory domainPropertyFactory(ObjectProvider<MappingContext> grailsDomainClassMappingContext) {
        return new DomainPropertyFactoryImpl(grailsDomainClassMappingContext.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainModelService domainModelService(DomainPropertyFactory domainPropertyFactory) {
        return new DomainModelServiceImpl(domainPropertyFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainInputRendererRegistry domainInputRendererRegistry() {
        return new DomainInputRendererRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainOutputRendererRegistry domainOutputRendererRegistry() {
        return new DomainOutputRendererRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainRendererRegisterer domainRendererRegisterer(DomainInputRendererRegistry domainInputRendererRegistry, DomainOutputRendererRegistry domainOutputRendererRegistry,
            ObjectProvider<LinkGenerator> grailsLinkGenerator) {
        return new DomainRendererRegisterer(domainInputRendererRegistry, domainOutputRendererRegistry, grailsLinkGenerator.getIfAvailable());
    }

}
