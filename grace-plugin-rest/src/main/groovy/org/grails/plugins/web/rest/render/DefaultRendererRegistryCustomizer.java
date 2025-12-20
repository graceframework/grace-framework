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
package org.grails.plugins.web.rest.render;

import org.springframework.validation.Errors;

import grails.core.support.proxy.ProxyHandler;
import grails.rest.render.RendererRegistry;
import grails.rest.render.RendererRegistryCustomizer;
import grails.web.mime.MimeType;
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer;
import org.grails.plugins.web.rest.render.json.DefaultJsonRenderer;
import org.grails.plugins.web.rest.render.xml.DefaultXmlRenderer;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;

/**
 * Default {@link RendererRegistryCustomizer} to initialize RendererRegistry.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
public class DefaultRendererRegistryCustomizer implements RendererRegistryCustomizer {

    private GrailsConventionGroovyPageLocator groovyPageLocator;
    private ProxyHandler proxyHandler;
    private String modelSuffix;

    public DefaultRendererRegistryCustomizer() {
    }

    public void setGroovyPageLocator(GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.groovyPageLocator = groovyPageLocator;
    }

    public void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    public void setModelSuffix(String modelSuffix) {
        this.modelSuffix = modelSuffix;
    }

    @Override
    public void customize(RendererRegistry rendererRegistry) {
        DefaultXmlRenderer<Object> defaultXmlRenderer = new DefaultXmlRenderer<>(Object.class);
        defaultXmlRenderer.setGroovyPageLocator(groovyPageLocator);
        defaultXmlRenderer.setRendererRegistry(rendererRegistry);
        rendererRegistry.addDefaultRenderer(defaultXmlRenderer);

        DefaultJsonRenderer<Object> defaultJsonRenderer = new DefaultJsonRenderer<>(Object.class);
        defaultJsonRenderer.setGroovyPageLocator(groovyPageLocator);
        defaultJsonRenderer.setRendererRegistry(rendererRegistry);
        rendererRegistry.addDefaultRenderer(defaultJsonRenderer);

        DefaultHtmlRenderer<Object> defaultHtmlRenderer = new DefaultHtmlRenderer<>(Object.class);
        defaultHtmlRenderer.setSuffix(modelSuffix);
        defaultHtmlRenderer.setProxyHandler(proxyHandler);
        rendererRegistry.addDefaultRenderer(defaultHtmlRenderer);

        DefaultHtmlRenderer<Object> allHtmlRenderer = new DefaultHtmlRenderer<>(Object.class, MimeType.ALL);
        allHtmlRenderer.setSuffix(modelSuffix);
        allHtmlRenderer.setProxyHandler(proxyHandler);
        rendererRegistry.addDefaultRenderer(allHtmlRenderer);

        DefaultXmlRenderer<Errors> defaultContainerXmlRenderer = new DefaultXmlRenderer<>(Errors.class, MimeType.XML, MimeType.TEXT_XML);
        defaultContainerXmlRenderer.setGroovyPageLocator(groovyPageLocator);
        defaultContainerXmlRenderer.setRendererRegistry(rendererRegistry);
        rendererRegistry.addContainerRenderer(Object.class, defaultContainerXmlRenderer);

        DefaultJsonRenderer<Errors> defaultContainerJsonRenderer = new DefaultJsonRenderer<>(Errors.class, MimeType.JSON, MimeType.TEXT_JSON);
        defaultContainerJsonRenderer.setGroovyPageLocator(groovyPageLocator);
        defaultContainerJsonRenderer.setRendererRegistry(rendererRegistry);
        rendererRegistry.addContainerRenderer(Object.class, defaultContainerJsonRenderer);

        DefaultHtmlRenderer<Errors> defaultContainerHtmlRenderer = new DefaultHtmlRenderer<>(Errors.class);
        defaultContainerHtmlRenderer.setSuffix(modelSuffix);
        defaultContainerHtmlRenderer.setProxyHandler(proxyHandler);
        rendererRegistry.addContainerRenderer(Object.class, defaultContainerHtmlRenderer);

        DefaultHtmlRenderer<Errors> defaultContainerAllRenderer = new DefaultHtmlRenderer<>(Errors.class, MimeType.ALL);
        defaultContainerAllRenderer.setSuffix(modelSuffix);
        defaultContainerAllRenderer.setProxyHandler(proxyHandler);
        rendererRegistry.addContainerRenderer(Object.class, defaultContainerAllRenderer);
    }

}
