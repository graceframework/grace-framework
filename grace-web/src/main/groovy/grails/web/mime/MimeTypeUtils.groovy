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
package grails.web.mime

import java.util.regex.Pattern

import jakarta.servlet.http.HttpServletRequest

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext

import grails.config.Config
import grails.config.Settings
import grails.core.GrailsApplication
import grails.web.http.HttpHeaders

import org.grails.core.lifecycle.ShutdownOperations
import org.grails.web.mime.DefaultAcceptHeaderParser
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes

/**
 * Utility methods for MimeType handling
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.4
 */
@CompileStatic
class MimeTypeUtils {

    // The ACCEPT header will not be used for content negotiation for user agents containing the following strings
    // (defaults to the 4 major rendering engines)
    static Pattern disableForUserAgents
    static boolean useAcceptHeaderXhr
    static boolean useAcceptHeader
    static {
        useDefaultConfig()
    }

    private static MimeType[] mimeTypes

    static {
        ShutdownOperations.addOperation({
            mimeTypes = null
            useDefaultConfig()
        }, true)
    }

    private static void useDefaultConfig() {
        disableForUserAgents = ~/(Gecko(?i)|WebKit(?i)|Presto(?i)|Trident(?i))/
        useAcceptHeaderXhr = true
        useAcceptHeader = true
    }

    @CompileStatic
    static MimeType[] getMimeTypes() {
        if (mimeTypes == null) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup()

            ApplicationContext context = webRequest.applicationContext
            if (context) {
                try {
                    mimeTypes = context.getBean(MimeUtility).getKnownMimeTypes() as MimeType[]
                    loadMimeTypeConfig(context.getBean(GrailsApplication).config)
                }
                catch (NoSuchBeanDefinitionException ignored) {
                    mimeTypes = MimeType.createDefaults()
                }
            }
            else {
                mimeTypes = MimeType.createDefaults()
            }
        }

        mimeTypes
    }

    @CompileStatic
    static String getResponseFormat(GrailsWebRequest webRequest) {
        HttpServletRequest request = webRequest.getCurrentRequest()
        def result = request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT)
        if (!result) {
            MimeType mimeType = getMimeTypeForRequest(webRequest)
            if (mimeType) {
                result = mimeType.extension
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, result)
            }
        }
        result
    }

    @CompileDynamic
    static MimeType[] getMimeTypesForRequest(GrailsWebRequest webRequest) {
        HttpServletRequest request = webRequest.getCurrentRequest()
        MimeType[] result = (MimeType[]) request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMATS)
        if (!result) {
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT)
            boolean msie = userAgent && userAgent ==~ /msie(?i)/ ?: false

            def parser = new DefaultAcceptHeaderParser(getMimeTypes())
            String header = null

            boolean disabledForUserAgent = !(useAcceptHeaderXhr && isAjaxRequest(request)) && disableForUserAgents != null &&
                    userAgent ? disableForUserAgents.matcher(userAgent).find() : false
            if (msie) {
                header = '*/*'
            }
            if (!header && useAcceptHeader && !disabledForUserAgent) {
                header = request.getHeader(HttpHeaders.ACCEPT)
            }
            result = parser.parse(header)

            // GRAILS-8341 - If no header the parser would have returned all configured mime types.  Since no format
            // was specified in the request we look for the 'all' format and return that if found.  If 'all' is
            // not found the fallback behavior is to return all configured mime types from the parser.
            if (!header) {
                for (mime in result) {
                    if (mime.extension == 'all') {
                        result = [mime] as MimeType[]
                        break
                    }
                }
            }

            request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMATS, result)
        }
        result
    }

    static MimeType getMimeTypeForRequest(GrailsWebRequest webRequest) {
        HttpServletRequest request = webRequest.getCurrentRequest()
        MimeType result = (MimeType) request.getAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE)
        if (!result) {
            def formatOverride = webRequest?.params?.format
            formatOverride = formatOverride ?: request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT)

            if (formatOverride) {
                MimeType[] allMimes = getMimeTypes()
                MimeType mime = allMimes?.find { MimeType it -> it.extension == formatOverride }
                result = mime ?: allMimes?.find { it }

                // Save the evaluated format as a request attribute.
                // This is a blatant hack because we should to this
                // on the first call. Unfortunately, doing so breaks
                // integration tests:
                //   - Test uses "c.params.format = ..."
                //   - "c.params" creates parameter map
                //   - which triggers the parameter parsing listeners
                //   - which call "request.format"
                //   - which initialises the CONTENT_FORMAT attribute
                //   - *before* the "format" parameter is added to the map
                //   - so the saved format is wrong
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, result)
            }
            else {
                result = getMimeTypesForRequest(webRequest)[0]
            }
        }
        result
    }

    static MimeType[] getMimeTypesFormatAware(GrailsWebRequest webRequest) {
        HttpServletRequest request = webRequest.getCurrentRequest()
        MimeType[] result = (MimeType[]) request.getAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPES)
        if (!result) {
            def formatOverride = webRequest?.params?.format
            formatOverride = formatOverride ?: request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT)

            if (formatOverride) {
                MimeType[] allMimes = getMimeTypes()
                MimeType mime = allMimes.find { MimeType it -> it.extension == formatOverride }
                result = [mime ?: getMimeTypes()[0]] as MimeType[]

                // Save the evaluated format as a request attribute.
                // This is a blatant hack because we should to this
                // on the first call. Unfortunately, doing so breaks
                // integration tests:
                //   - Test uses "c.params.format = ..."
                //   - "c.params" creates parameter map
                //   - which triggers the parameter parsing listeners
                //   - which call "request.format"
                //   - which initialises the CONTENT_FORMAT attribute
                //   - *before* the "format" parameter is added to the map
                //   - so the saved format is wrong
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPES, result)
            }
            else {
                result = getMimeTypesForRequest(webRequest)
            }
        }
        result
    }

    static MimeType resolveMimeType(Object source, MimeTypeResolver mimeTypeResolver) {
        MimeType mimeType
        if (mimeTypeResolver) {
            MimeType resolvedMimeType = mimeTypeResolver.resolveRequestMimeType()
            mimeType = resolvedMimeType ?: MimeType.ALL
        }
        else if (source instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) source
            String contentType = req.contentType
            if (contentType != null) {
                mimeType = new MimeType(contentType)
            }
            else {
                mimeType = MimeType.ALL
            }
        }
        else {
            mimeType = MimeType.ALL
        }
        mimeType
    }

    private static void loadMimeTypeConfig(Config config) {
        useAcceptHeader = config.getProperty(Settings.MIME_USE_ACCEPT_HEADER, Boolean, true)

        if (config.containsKey(Settings.MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS_XHR)) {
            boolean disableForUserAgentsXhrConfig = config.getProperty(Settings.MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS_XHR,
                    Boolean, false)
            // if MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS_XHR is set to true, we want xhr's to check the user agent list.
            useAcceptHeaderXhr = !disableForUserAgentsXhrConfig
        }
        if (config.containsKey(Settings.MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS)) {
            Object disableForUserAgentsConfig = config.getProperty(Settings.MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS, Object)
            if (disableForUserAgentsConfig instanceof Pattern) {
                disableForUserAgents = (Pattern) disableForUserAgentsConfig
            }
            else if (disableForUserAgentsConfig instanceof Collection && disableForUserAgentsConfig) {
                String userAgents = disableForUserAgentsConfig.join('(?i)|')
                disableForUserAgents = Pattern.compile("(${userAgents})")
            }
            else {
                disableForUserAgents = null
            }
        }
    }

    private static boolean isAjaxRequest(HttpServletRequest request) {
        request.getHeader('X-Requested-With') == 'XMLHttpRequest'
    }

}
