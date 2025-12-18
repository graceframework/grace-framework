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
package org.grails.web.mapping.mvc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import grails.web.mapping.UrlMappingInfo;
import grails.web.mapping.UrlMappingsHolder;
import grails.web.mapping.exceptions.UrlMappingException;

import org.grails.core.exceptions.GrailsRuntimeException;
import org.grails.web.errors.GrailsExceptionResolver;
import org.grails.web.mapping.DefaultUrlMappingInfo;
import org.grails.web.mapping.UrlMappingUtils;
import org.grails.web.util.WebUtils;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver} implementation
 * that allows for mapping exception class names to view names, either for a set of
 * given url mappings.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GrailsUrlMappingsExceptionResolver extends GrailsExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ModelAndView mv = super.resolveException(request, response, handler, ex);

        UrlMappingsHolder urlMappings = lookupUrlMappings();
        if (urlMappings != null) {
            mv = resolveViewOrForward(ex, urlMappings, request, response, mv);
        }

        return mv;
    }

    protected UrlMappingsHolder lookupUrlMappings() {
        try {
            return UrlMappingUtils.lookupUrlMappings(this.servletContext);
        }
        catch (Exception ignored) {
            // ignore, no app ctx in this case.
            return null;
        }
    }

    Map extractRequestParamsWithUrlMappingHolder(UrlMappingsHolder urlMappings, HttpServletRequest request) {
        Map params = new HashMap();
        try {
            UrlMappingInfo requestInfo = urlMappings.match(request.getRequestURI());
            if (requestInfo != null) {
                params.putAll(UrlMappingUtils.findAllParamsNotInUrlMappingKeywords(requestInfo.getParameters()));
            }
        }
        catch (UrlMappingException ulrMappingException) {
            logger.debug("Could not find urlMapping which matches: " + request.getRequestURI());
        }
        return params;
    }

    protected ModelAndView resolveViewOrForward(Exception ex, UrlMappingsHolder urlMappings, HttpServletRequest request,
            HttpServletResponse response, ModelAndView mv) {
        UrlMappingInfo info = matchStatusCode(ex, urlMappings);

        if (info != null) {
            Map params = extractRequestParamsWithUrlMappingHolder(urlMappings, request);
            if (params != null && !params.isEmpty()) {
                Map infoParams = info.getParameters();
                if (infoParams != null) {
                    params.putAll(info.getParameters());
                }
                info = new DefaultUrlMappingInfo(info, params, this.grailsApplication);
            }
        }

        try {
            if (info != null && info.getViewName() != null) {
                resolveView(request, info, mv);
            }
            else if (info != null && info.getControllerName() != null) {
                String uri = determineUri(request);
                if (!response.isCommitted()) {
                    forwardRequest(info, request, response, mv, uri);
                    // return an empty ModelAndView since the error handler has been processed
                    return new ModelAndView();
                }
            }
            return mv;
        }
        catch (Exception e) {
            logger.error("Unable to render errors view: " + e.getMessage(), e);
            throw new GrailsRuntimeException(e);
        }
    }

    protected void forwardRequest(UrlMappingInfo info, HttpServletRequest request, HttpServletResponse response,
            ModelAndView mv, String uri) throws ServletException, IOException {

        info.configure(WebUtils.retrieveGrailsWebRequest());
        String forwardUrl = UrlMappingUtils.forwardRequestForUrlMappingInfo(
                request, response, info, mv.getModel(), true);
        if (logger.isDebugEnabled()) {
            logger.debug("Matched URI [" + uri + "] to URL mapping [" + info +
                    "], forwarding to [" + forwardUrl + "] with response [" + response.getClass() + "]");
        }
    }

    protected String determineUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return uri;
    }

    protected void resolveView(HttpServletRequest request, UrlMappingInfo info, ModelAndView mv) throws Exception {
        ViewResolver viewResolver = WebUtils.lookupViewResolver(this.servletContext);
        View v = UrlMappingUtils.resolveView(request, info, info.getViewName(), viewResolver);
        if (v != null) {
            mv.setView(v);
        }
    }

    protected UrlMappingInfo matchStatusCode(Exception ex, UrlMappingsHolder urlMappings) {
        UrlMappingInfo info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        if (info == null) {
            info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    getRootCause(ex));
        }
        if (info == null) {
            info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return info;
    }

}
