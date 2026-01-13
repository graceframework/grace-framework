/*
 * Copyright 2011-2026 the original author or authors.
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
package org.grails.web.async

import com.opensymphony.sitemesh.Content
import com.opensymphony.sitemesh.Decorator
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncListener
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext
import org.springframework.context.ApplicationContext

import grails.web.async.GrailsAsyncWebRequest
import grails.persistence.support.PersistenceContextInterceptor

import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.sitemesh.GrailsContentBufferingResponse
import org.grails.web.sitemesh.GroovyPageLayoutFinder
import org.grails.web.util.WebUtils

/**
 * Wraps an AsyncContext providing additional logic to provide the appropriate context to a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class GrailsAsyncContext implements AsyncContext {

    private static final String PERSISTENCE_INTERCEPTORS = 'org.codehaus.groovy.grails.PERSISTENCE_INTERCEPTORS'

    final @Delegate AsyncContext delegate
    final GrailsWebRequest originalWebRequest
    final GroovyPageLayoutFinder groovyPageLayoutFinder
    final GrailsAsyncWebRequest asyncGrailsWebRequest

    GrailsAsyncContext(AsyncContext delegate, GrailsWebRequest webRequest, GrailsAsyncWebRequest asyncGrailsWebRequest = null) {
        this.delegate = delegate
        this.originalWebRequest = webRequest
        ApplicationContext applicationContext = webRequest.getApplicationContext()
        if (applicationContext && applicationContext.containsBean('groovyPageLayoutFinder')) {
            this.groovyPageLayoutFinder = applicationContext.getBean('groovyPageLayoutFinder', GroovyPageLayoutFinder)
        }
        this.asyncGrailsWebRequest = asyncGrailsWebRequest
    }

    <T extends AsyncListener> T createListener(Class<T> tClass) {
        delegate.createListener(tClass)
    }

    void start(Runnable runnable) {
        delegate.start {
            GrailsWebRequest webRequest =  asyncGrailsWebRequest ?:
                    new GrailsWebRequest((HttpServletRequest) request, (HttpServletResponse) response, request.getServletContext())
            WebUtils.storeGrailsWebRequest(webRequest)
            Collection<PersistenceContextInterceptor> interceptors = getPersistenceInterceptors(webRequest)

            for (PersistenceContextInterceptor i in interceptors) {
                i.init()
            }
            try {
                runnable.run()
            }
            finally {
                for (PersistenceContextInterceptor i in interceptors) {
                    i.destroy()
                }
                webRequest.requestCompleted()
                WebUtils.clearGrailsWebRequest()
            }
        }
    }

    void complete() {
        if (response instanceof GrailsContentBufferingResponse) {
            GrailsContentBufferingResponse bufferingResponse = (GrailsContentBufferingResponse) response
            HttpServletResponse targetResponse = bufferingResponse.getTargetResponse()
            Content content = bufferingResponse.getContent()
            HttpServletRequest httpRequest = (HttpServletRequest) request
            if (content != null && groovyPageLayoutFinder != null) {
                Decorator decorator = (Decorator) groovyPageLayoutFinder?.findLayout(httpRequest, content)
                if (decorator) {
                    decorator.render(content, new SiteMeshWebAppContext(httpRequest, targetResponse, request.servletContext))
                }
                else {
                    content.writeOriginal(targetResponse.getWriter())
                }
            }
        }
        delegate.complete()
     }

    protected Collection<PersistenceContextInterceptor> getPersistenceInterceptors(GrailsWebRequest webRequest) {
        ServletContext servletContext = webRequest.servletContext
        Collection<PersistenceContextInterceptor> interceptors =
                (Collection<PersistenceContextInterceptor>) servletContext?.getAttribute(PERSISTENCE_INTERCEPTORS)
        if (interceptors == null) {
            interceptors = webRequest.applicationContext?.getBeansOfType(PersistenceContextInterceptor)?.values() ?: []
            servletContext.setAttribute(PERSISTENCE_INTERCEPTORS, interceptors)
        }
        return interceptors
    }

}
