/*
 * Copyright 2013-2026 the original author or authors.
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
package org.grails.plugins.web.async.mvc

import jakarta.servlet.AsyncContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import groovy.transform.CompileStatic
import org.springframework.web.context.request.async.WebAsyncManager
import org.springframework.web.context.request.async.WebAsyncUtils
import org.springframework.web.servlet.ModelAndView

import grails.async.Promise
import grails.async.PromiseList
import grails.web.async.GrailsAsyncWebRequest

import org.grails.web.async.GrailsAsyncContext
import org.grails.web.errors.GrailsExceptionResolver
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.ActionResultTransformer
import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Handles an Async response from a controller
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.3
 */
@CompileStatic
class AsyncActionResultTransformer implements ActionResultTransformer {

    private GrailsExceptionResolver exceptionResolver

    void setExceptionResolver(GrailsExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver
    }

    @Override
    Object transformActionResult(GrailsWebRequest webRequest, String viewName, Object actionResult) {
        if (actionResult instanceof Promise) {
            HttpServletRequest request = webRequest.getCurrentRequest()
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request)
            HttpServletResponse response = webRequest.getResponse()

            GrailsAsyncWebRequest asyncWebRequest
            if (asyncManager.isConcurrentHandlingStarted()) {
                asyncWebRequest = GrailsAsyncWebRequest.lookup(request)
                if (asyncWebRequest == null) {
                    throw new IllegalStateException('Concurrency handling already started by another process')
                }
            }
            else {
                asyncWebRequest = new GrailsAsyncWebRequest(request, response, webRequest.servletContext)
                asyncManager.setAsyncWebRequest(asyncWebRequest)
                asyncWebRequest.startAsync()
            }

            AsyncContext asyncContext = asyncWebRequest.asyncContext
            request.setAttribute(GrailsApplicationAttributes.ASYNC_STARTED, true)
            asyncContext = new GrailsAsyncContext(asyncContext, webRequest)

            asyncContext.start {
                Promise p = (Promise) actionResult
                if (p instanceof PromiseList) {
                    p.onComplete { List results ->
                        handleComplete(request, response, asyncContext)
                    }
                }
                else {
                    p.onComplete {
                        if (it instanceof Map) {
                            ModelAndView modelAndView = new ModelAndView(viewName, it)
                            asyncContext.getRequest().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView)

                            asyncContext.dispatch()
                        }
                        else {
                            Object modelAndView = asyncContext.getRequest().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                            if (modelAndView) {
                                asyncContext.dispatch()
                            }
                            else {
                                handleComplete(request, response, asyncContext)
                            }
                        }
                    }
                }
                p.onError { Throwable t ->
                    if (response.isCommitted()) {
                        asyncContext.complete()
                    }
                    else {
                        GrailsExceptionResolver exceptionResolver = createExceptionResolver(webRequest)
                        request.setAttribute(GrailsExceptionResolver.EXCEPTION_ATTRIBUTE, t)
                        ModelAndView modelAndView = exceptionResolver.resolveException(request, response, this, (Exception) t)
                        asyncContext.getRequest().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView)
                        asyncContext.dispatch()
                    }
                }
            }
            return null
        }
        return actionResult
    }

    protected void handleComplete(HttpServletRequest request, HttpServletResponse response, AsyncContext asyncContext) {
        asyncContext.complete()
    }

    private GrailsExceptionResolver createExceptionResolver(GrailsWebRequest webRequest) {
        if (!this.exceptionResolver) {
            this.exceptionResolver = new GrailsExceptionResolver()
            this.exceptionResolver.servletContext = webRequest.servletContext
            this.exceptionResolver.grailsApplication = webRequest.attributes.grailsApplication
            this.exceptionResolver.mappedHandlers = [this] as Set
            Properties properties = new Properties()
            properties['java.lang.Exception'] = '/error'
            this.exceptionResolver.exceptionMappings = properties
        }
        return this.exceptionResolver
    }

}
