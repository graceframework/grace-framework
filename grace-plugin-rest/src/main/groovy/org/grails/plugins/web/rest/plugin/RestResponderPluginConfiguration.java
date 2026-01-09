/*
 * Copyright 2021-2026 the original author or authors.
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
package org.grails.plugins.web.rest.plugin;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.core.support.proxy.ProxyHandler;
import grails.rest.render.Renderer;
import grails.rest.render.RendererRegistry;
import grails.rest.render.RendererRegistryCustomizer;

import org.grails.plugins.web.rest.render.DefaultRendererRegistryCustomizer;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;
import org.grails.web.rest.render.DefaultRendererRegistry;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Grails RestResponder Plugin.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RestResponderPluginConfiguration {

    @Bean
    public RendererRegistry rendererRegistry(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<GrailsConventionGroovyPageLocator> groovyPageLocatorObjectProvider,
            ObjectProvider<ProxyHandler> proxyHandlerObjectProvider,
            ObjectProvider<Renderer<?>> rendererObjectProvider,
            ObjectProvider<RendererRegistryCustomizer> rendererRegistryCustomizers) {
        Config config = grailsApplication.getIfAvailable().getConfig();
        String modelSuffix = config.getProperty(Settings.SCAFFOLDING_DOMAIN_SUFFIX, "");

        GrailsConventionGroovyPageLocator groovyPageLocator = groovyPageLocatorObjectProvider.getIfAvailable();
        ProxyHandler proxyHandler = proxyHandlerObjectProvider.getIfAvailable();

        DefaultRendererRegistry rendererRegistry = new DefaultRendererRegistry();
        rendererRegistry.setModelSuffix(modelSuffix);
        rendererRegistry.setGroovyPageLocator(groovyPageLocator);
        rendererRegistry.setProxyHandler(proxyHandler);

        List<Renderer<?>> renderers = rendererObjectProvider.stream().toList();
        rendererRegistry.setRenderers(renderers.toArray(Renderer[]::new));

        applyCustomizers(rendererRegistry, rendererRegistryCustomizers);

        return rendererRegistry;
    }

    private void applyCustomizers(DefaultRendererRegistry rendererRegistry,
            ObjectProvider<RendererRegistryCustomizer> rendererRegistryCustomizers) {
        Iterable<RendererRegistryCustomizer> orderedCustomizers = () -> rendererRegistryCustomizers.orderedStream()
                .iterator();
        for (RendererRegistryCustomizer customizer : orderedCustomizers) {
            customizer.customize(rendererRegistry);
        }
    }

    @Bean
    public RendererRegistryCustomizer defaultRendererRegistryCustomizer(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<GrailsConventionGroovyPageLocator> groovyPageLocatorObjectProvider,
            ObjectProvider<ProxyHandler> proxyHandlerObjectProvider) {
        Config config = grailsApplication.getIfAvailable().getConfig();
        String modelSuffix = config.getProperty(Settings.SCAFFOLDING_DOMAIN_SUFFIX, "");

        DefaultRendererRegistryCustomizer rendererRegistryCustomizer = new DefaultRendererRegistryCustomizer();
        rendererRegistryCustomizer.setGroovyPageLocator(groovyPageLocatorObjectProvider.getIfAvailable());
        rendererRegistryCustomizer.setProxyHandler(proxyHandlerObjectProvider.getIfAvailable());
        rendererRegistryCustomizer.setModelSuffix(modelSuffix);
        return rendererRegistryCustomizer;
    }

}
