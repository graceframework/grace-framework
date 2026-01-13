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
package org.grails.plugins.web.async

import jakarta.servlet.AsyncContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.TimeoutException

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.async.WebAsyncManager
import org.springframework.web.context.request.async.WebAsyncUtils

import grails.async.decorator.PromiseDecorator
import grails.web.async.GrailsAsyncWebRequest
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils

/**
 * A promise decorated lookup strategy that binds a WebRequest to the promise thread
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AsyncWebRequestPromiseDecorator implements PromiseDecorator {

    GrailsWebRequest webRequest
    final GrailsAsyncWebRequest asyncRequest
    final AsyncContext asyncContext
    volatile boolean timeoutReached = false

    AsyncWebRequestPromiseDecorator(GrailsWebRequest webRequest) {
        this.webRequest = webRequest
        HttpServletRequest currentServletRequest = webRequest.currentRequest
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(currentServletRequest)
        GrailsAsyncWebRequest newWebRequest
        if (asyncManager.isConcurrentHandlingStarted()) {
            newWebRequest = GrailsAsyncWebRequest.lookup(currentServletRequest)
            this.asyncContext = newWebRequest.asyncContext
            if (newWebRequest == null || newWebRequest.isAsyncComplete()) {
                throw new IllegalStateException('Cannot start a task once asynchronous request processing has completed')
            }
        }
        else {
            newWebRequest = new GrailsAsyncWebRequest(currentServletRequest, webRequest.currentResponse,
                    webRequest.servletContext, webRequest.applicationContext)
            asyncManager.setAsyncWebRequest(newWebRequest)
            newWebRequest.startAsync()
            this.asyncContext = newWebRequest.asyncContext
            this.asyncContext.setTimeout(-1)
        }
        newWebRequest.addTimeoutHandler({
            this.timeoutReached = true
        })
        this.asyncRequest = newWebRequest
    }

    @Override
    <D> Closure<D> decorate(Closure<D> c) {
        return (Closure<D>) {  args ->
            if (this.timeoutReached) {
                throw new TimeoutException('Asynchronous request processing timeout reached')
            }
            HttpServletRequest request = (HttpServletRequest) this.asyncContext.request
            if (request.isAsyncStarted()) {
                WebUtils.storeGrailsWebRequest(
                        new GrailsWebRequest(request, (HttpServletResponse) this.asyncContext.response, webRequest.attributes))
                try {
                    return invokeClosure(c, args)
                }
                finally {
                    RequestContextHolder.resetRequestAttributes()
                }
            }
            else {
                throw new IllegalStateException('Asynchronous request already terminated. Likely timeout reached')
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def invokeClosure(Closure c, args) {
        if (args == null) {
            c.call(null)
        }
        else if (args && args.getClass().isArray()) {
            c.call(*args)
        }
        else if (args instanceof List) {
            c.call(*args)
        }
        else {
            c.call(args)
        }
    }

}
