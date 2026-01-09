/*
 * Copyright 2014-2026 the original author or authors.
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
package org.grails.web.mime

import jakarta.servlet.http.HttpServletResponse

import groovy.transform.CompileStatic

import grails.web.mime.MimeType
import grails.web.mime.MimeTypeUtils

import org.grails.plugins.web.api.MimeTypesApiSupport
import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 *
 * Extends the {@link HttpServletResponse} object with new methods for handling {@link MimeType} instances
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class HttpServletResponseExtension {

    static MimeTypesApiSupport apiSupport = new MimeTypesApiSupport()

    /**
     * Obtains the format to use for the response using either the file extension or the ACCEPT header
     *
     * @param response The response
     * @return The request format
     */
    @CompileStatic
    static String getFormat(HttpServletResponse response) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        MimeTypeUtils.getResponseFormat(webRequest)
    }

    /**
     * Obtains the MimeType for the response using either the file extension or the ACCEPT header
     *
     * @param response The response
     * @return The MimeType
     */
    static MimeType getMimeType(HttpServletResponse response) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        MimeTypeUtils.getMimeTypeForRequest(webRequest)
    }

    /**
     * Gets the configured mime types for the response
     *
     * @param response The response
     * @return The configured mime types
     */
    static MimeType[] getMimeTypes(HttpServletResponse response) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        MimeTypeUtils.getMimeTypesForRequest(webRequest)
    }

    /**
     * Gets the configured mime types for the response
     *
     * @param response The response
     * @return The configured mime types
     */
    static MimeType[] getMimeTypesFormatAware(HttpServletResponse response) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        MimeTypeUtils.getMimeTypesFormatAware(webRequest)
    }

    /**
     * Allows for the response.withFormat { } syntax
     *
     * @param response The response
     * @param callable A closure
     * @return The result of the closure call
     */
    static Object withFormat(HttpServletResponse response, Closure callable) {
        apiSupport.withFormat(response, callable)
    }

}
