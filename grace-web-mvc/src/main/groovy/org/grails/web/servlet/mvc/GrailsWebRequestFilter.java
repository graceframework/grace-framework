/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.web.servlet.mvc;

import java.io.IOException;
import java.util.Collection;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import grails.web.mvc.FlashScope;

import org.grails.web.util.WebUtils;

/**
 * Binds a {@link GrailsWebRequestFilter} to the currently executing thread.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsWebRequestFilter extends OncePerRequestFilter {

    private Collection<ParameterCreationListener> parameterCreationListeners;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        LocaleContextHolder.setLocale(request.getLocale());
        response = new OutputAwareHttpServletResponse(response);

        boolean isIncludeOrForward = WebUtils.isForwardOrInclude(request);
        GrailsWebRequest previous = isIncludeOrForward ? GrailsWebRequest.lookup(request) : null;
        GrailsWebRequest webRequest = new GrailsWebRequest(request, response, getServletContext());
        configureParameterCreationListeners(webRequest);

        if (logger.isDebugEnabled()) {
            logger.debug("Bound Grails request context to thread: " + request);
        }

        try {
            WebUtils.storeGrailsWebRequest(webRequest);

            if (!isIncludeOrForward) {
                // Set the flash scope instance to its next state. We do
                // this here so that the flash is available from Grails
                // filters in a valid state.
                FlashScope fs = webRequest.getAttributes().getFlashScope(request);
                fs.next();
            }

            // Pass control on to the next filter (or the servlet if
            // there are no more filters in the chain).
            filterChain.doFilter(request, response);
        }
        finally {
            webRequest.requestCompleted();

            if (isIncludeOrForward) {
                if (previous != null) {
                    WebUtils.storeGrailsWebRequest(previous);
                }
            }
            else {

                WebUtils.clearGrailsWebRequest();
                LocaleContextHolder.setLocale(null);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Cleared Grails thread-bound request context: " + request);
            }
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private void configureParameterCreationListeners(GrailsWebRequest webRequest) {
        if (this.parameterCreationListeners != null) {
            for (ParameterCreationListener creationListenerBean : this.parameterCreationListeners) {
                webRequest.addParameterListener(creationListenerBean);
            }
        }
        else {
            logger.warn("parameterCreationListeners isn't initialized.");
        }
    }

    public void setParameterCreationListeners(Collection<ParameterCreationListener> parameterCreationListeners) {
        this.parameterCreationListeners = parameterCreationListeners;
    }

}
