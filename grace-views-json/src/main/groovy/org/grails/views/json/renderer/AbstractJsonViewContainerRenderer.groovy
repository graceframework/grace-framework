/*
 * Copyright 2015-2026 the original author or authors.
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
package org.grails.views.json.renderer

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import grails.rest.render.AbstractRenderer
import grails.rest.render.ContainerRenderer
import grails.rest.render.RenderContext
import grails.util.GrailsNameUtils
import grails.util.GrailsWebUtil
import grails.views.Views
import grails.web.mime.MimeType

import org.grails.views.json.mvc.JsonViewResolver
import org.grails.web.rest.render.ServletRenderContext

/**
 * A container renderer that looks up JSON views
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
@InheritConstructors
abstract class AbstractJsonViewContainerRenderer<T, C> extends AbstractRenderer<T> implements ContainerRenderer<T, C> {

    private static final MimeType[] DEFAULT_MIME_TYPES = [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    String encoding = GrailsWebUtil.DEFAULT_ENCODING

    private JsonViewResolver jsonViewResolver

    AbstractJsonViewContainerRenderer(Class<T> targetType, String encoding) {
        super(targetType, DEFAULT_MIME_TYPES)
        this.encoding = encoding
    }

    AbstractJsonViewContainerRenderer(Class<T> targetType, MimeType[] mimeTypes, String encoding) {
        super(targetType, mimeTypes)
        this.encoding = encoding
    }

    void setJsonViewResolver(JsonViewResolver jsonViewResolver) {
        this.jsonViewResolver = jsonViewResolver
    }

    @Override
    void render(T object, RenderContext context) {
        String viewUri = "/${context.controllerName}/_${GrailsNameUtils.getPropertyName(targetType)}"
        def webRequest = ((ServletRenderContext) context).getWebRequest()
        def request = webRequest.currentRequest
        def response = webRequest.currentResponse

        if (webRequest.controllerNamespace) {
            viewUri = "/${webRequest.controllerNamespace}" + viewUri
        }
        def view = jsonViewResolver.resolveView(viewUri, context.locale)
        if (view == null) {
            view = jsonViewResolver.resolveView(targetType, context.locale)
        }

        if (view != null) {
            Map<String, Object> model = (Map<String, Object>) [(resolveModelName()): object]
            def contextArguments = context.getArguments()
            def contextModel = contextArguments?.get(Views.MODEL)
            if (contextModel instanceof Map) {
                model.putAll((Map) contextModel)
            }

            view.render(model, request, response)
        } else {
            // 404 Not found
            def notFound = jsonViewResolver.resolveView('/notFound', context.locale)
            if (notFound != null) {
                notFound.render([:], request, response)
            }
            else {
                def error = jsonViewResolver.resolveView('/error', context.locale)
                if (error != null) {
                    error.render([:], request, response)
                }
            }
        }
    }

    protected String resolveModelName() {
        GrailsNameUtils.getPropertyName(targetType)
    }

}
