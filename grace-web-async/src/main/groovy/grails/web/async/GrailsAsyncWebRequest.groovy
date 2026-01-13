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
package grails.web.async

import groovy.transform.CompileStatic
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import org.springframework.context.ApplicationContext
import org.springframework.util.Assert
import org.springframework.web.context.request.async.AsyncWebRequest

import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Implementation of Spring 4.0 {@link AsyncWebRequest} interface for Grails
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsAsyncWebRequest extends GrailsWebRequest implements AsyncWebRequest, AsyncListener {

    static final String WEB_REQUEST = 'org.grails.ASYNC_WEB_REQUEST'

    private final AtomicBoolean asyncCompleted = new AtomicBoolean(false)

    Long timeout
    AsyncContext asyncContext

    List<Runnable> timeoutHandlers = []
    List<Runnable> completionHandlers = []
    List<Consumer<Throwable>> exceptionHandlers = []

    GrailsAsyncWebRequest(HttpServletRequest request, HttpServletResponse response, GrailsApplicationAttributes attributes) {
        super(request, response, attributes)
        request.setAttribute(WEB_REQUEST, this)
    }

    GrailsAsyncWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        super(request, response, servletContext)
        request.setAttribute(WEB_REQUEST, this)
    }

    GrailsAsyncWebRequest(HttpServletRequest request, HttpServletResponse response,
                          ServletContext servletContext, ApplicationContext applicationContext) {
        super(request, response, servletContext, applicationContext)
        request.setAttribute(WEB_REQUEST, this)
    }

    /**
     * Looks up the GrailsWebRequest from the current request.
     * @param request The current request
     * @return The GrailsWebRequest
     */
    static GrailsAsyncWebRequest lookup(HttpServletRequest request) {
        GrailsAsyncWebRequest webRequest = (GrailsAsyncWebRequest) request.getAttribute(WEB_REQUEST)
        return webRequest
    }

    @Override
    void addTimeoutHandler(Runnable runnable) {
        this.timeoutHandlers << runnable
    }

    @Override
    void addErrorHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandlers << exceptionHandler
    }

    @Override
    void addCompletionHandler(Runnable runnable) {
        this.completionHandlers << runnable
    }

    @Override
    void startAsync() {
        Assert.state(request.asyncSupported, 'The current request does not support Async processing')
        if (!isAsyncStarted()) {
            this.asyncContext = request.startAsync(request, response)
            this.asyncContext.addListener(this)
        }
    }

    @Override
    boolean isAsyncStarted() {
        this.asyncContext && request.asyncStarted
    }

    @Override
    void dispatch() {
        Assert.notNull this.asyncContext, 'Cannot dispatch without an AsyncContext'
        this.asyncContext.dispatch()
    }

    @Override
    boolean isAsyncComplete() {
        return this.asyncCompleted.get()
    }

    @Override
    void onComplete(AsyncEvent event) throws IOException {
        for (handler in completionHandlers) {
            handler.run()
        }
        this.asyncContext = null
        this.asyncCompleted.set true
    }

    @Override
    void onTimeout(AsyncEvent event) throws IOException {
        for (handler in timeoutHandlers) {
            handler.run()
        }
    }

    @Override
    void onError(AsyncEvent event) throws IOException {
    }

    @Override
    void onStartAsync(AsyncEvent event) throws IOException {
    }

}
