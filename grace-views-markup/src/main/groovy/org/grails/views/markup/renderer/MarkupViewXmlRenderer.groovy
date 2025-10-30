/*
 * Copyright 2015-2025 the original author or authors.
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
package org.grails.views.markup.renderer

import grails.core.support.proxy.ProxyHandler
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.views.mvc.SmartViewResolver
import grails.views.mvc.renderer.DefaultViewRenderer
import grails.web.mime.MimeType

/**
 * Integration with the Grails renderer framework
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
class MarkupViewXmlRenderer<T> extends DefaultViewRenderer<T> {

    MarkupViewXmlRenderer(Class<T> targetType, SmartViewResolver viewResolver, ProxyHandler proxyHandler,
                          RendererRegistry rendererRegistry, Renderer defaultRenderer) {
        super(targetType, MimeType.XML, viewResolver, proxyHandler, rendererRegistry, defaultRenderer)
    }

    MarkupViewXmlRenderer(Class<T> targetType, MimeType mimeType, SmartViewResolver viewResolver, ProxyHandler proxyHandler,
                          RendererRegistry rendererRegistry, Renderer defaultRenderer) {
        super(targetType, mimeType, viewResolver, proxyHandler, rendererRegistry, defaultRenderer)
    }

}
