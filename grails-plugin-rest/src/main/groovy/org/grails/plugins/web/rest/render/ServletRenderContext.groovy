/*
 * Copyright 2012-2023 the original author or authors.
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
package org.grails.plugins.web.rest.render

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.ModelAndView

import grails.rest.render.AbstractRenderContext
import grails.web.mime.MimeType

import org.grails.core.util.IncludeExcludeSupport
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.util.WebUtils

/**
 * RenderContext for the servlet environment
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class ServletRenderContext extends AbstractRenderContext {

    GrailsWebRequest webRequest
    Map<String, Object> arguments
    private String resourcePath
    private boolean writerObtained = false

    ServletRenderContext(GrailsWebRequest webRequest) {
        this(webRequest, Collections.<String, Object> emptyMap())
    }

    ServletRenderContext(GrailsWebRequest webRequest, Map<String, Object> arguments) {
        this.webRequest = webRequest
        if (arguments != null) {
            this.arguments = Collections.unmodifiableMap(arguments)
            Map<String, Object> argsMap = arguments
            Object incObject = argsMap != null ?  argsMap.get(IncludeExcludeSupport.INCLUDES_PROPERTY) : null
            Object excObject = argsMap != null ? argsMap.get(IncludeExcludeSupport.EXCLUDES_PROPERTY) : null
            List<String> includes = incObject instanceof List ? (List<String>) incObject : null
            List<String> excludes = excObject instanceof List ? (List<String>) excObject : null
            if (includes != null) {
                this.includes = includes
            }
            if (excludes != null) {
                this.excludes = excludes
            }
        }
        else {
            this.arguments = Collections.<String, Object> emptyMap()
        }
    }

    @Override
    String getResourcePath() {
        if (this.resourcePath == null) {
            return WebUtils.getForwardURI(this.webRequest.request)
        }
        this.resourcePath
    }

    void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    MimeType getAcceptMimeType() {
        HttpServletResponse response = this.webRequest.response
        response.hasProperty('mimeType') ? response.mimeType : null
    }

    @Override
    Locale getLocale() {
        this.webRequest.locale
    }

    @Override
    Writer getWriter() {
        this.writerObtained = true
        this.webRequest.currentResponse.writer
    }

    @Override
    HttpMethod getHttpMethod() {
        HttpMethod.valueOf(this.webRequest.currentRequest.method)
    }

    @Override
    void setStatus(HttpStatus status) {
        this.webRequest.currentResponse.setStatus(status.value())
    }

    @Override
    void setContentType(String contentType) {
        this.webRequest.currentResponse.contentType = contentType
    }

    @Override
    void setViewName(String viewName) {
        ModelAndView modelAndView = getModelAndView()
        modelAndView.setViewName(viewName)
    }

    @Override
    String getViewName() {
        HttpServletRequest request = this.webRequest.currentRequest
        ModelAndView modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        modelAndView ? modelAndView.viewName : null
    }

    @Override
    String getDefaultViewName() {
        String namespace = getControllerNamespace()
        String controller = getControllerName()
        String viewName = getActionName()
        if (namespace) {
            viewName = namespace + '/' + controller + '/' + viewName
        }
        else {
            viewName = controller + '/' + viewName
        }
        viewName
    }

    protected ModelAndView getModelAndView() {
        HttpServletRequest request = this.webRequest.currentRequest
        ModelAndView modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        if (modelAndView == null) {
            modelAndView = new ModelAndView()
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView)
        }
        modelAndView
    }

    @Override
    void setModel(Map model) {
        ModelAndView modelAndView = getModelAndView()
        Map<String, Object> viewModel = modelAndView.model
        if (this.arguments?.model instanceof Map) {
            viewModel.putAll((Map) this.arguments.model)
        }
        viewModel.putAll(model)
    }

    @Override
    String getActionName() {
        this.webRequest.actionName
    }

    @Override
    String getControllerName() {
        this.webRequest.controllerName
    }

    @Override
    String getControllerNamespace() {
        this.webRequest.controllerNamespace
    }

    @Override
    boolean wasWrittenTo() {
        this.writerObtained
    }

}
