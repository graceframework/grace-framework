/*
 * Copyright 2011-2023 the original author or authors.
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
package org.grails.web.mapping

import java.util.regex.Pattern

import jakarta.annotation.PostConstruct

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod

import grails.config.Settings
import grails.plugins.GrailsPluginManager
import grails.plugins.PluginManagerAware
import grails.util.Environment
import grails.util.GrailsClassUtils
import grails.util.GrailsNameUtils
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import grails.web.mapping.LinkGenerator
import grails.web.mapping.UrlCreator
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingsHolder

import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.web.servlet.mvc.DefaultRequestStateLookupStrategy
import org.grails.web.servlet.mvc.GrailsRequestStateLookupStrategy
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils

/**
 * A link generating service for applications to use when generating links.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
@Slf4j
class DefaultLinkGenerator implements LinkGenerator, PluginManagerAware {

    private static final Pattern ABSOLUTE_URL_PATTERN = Pattern.compile('^[A-Za-z][A-Za-z0-9+\\-.]*:.*$')

    String configuredServerBaseURL
    String contextPath
    String resourcePath

    GrailsRequestStateLookupStrategy requestStateLookupStrategy = new DefaultRequestStateLookupStrategy()

    GrailsPluginManager pluginManager

    @Autowired
    @Qualifier('grailsUrlMappingsHolder')
    UrlMappingsHolder urlMappingsHolder

    @Autowired(required = false)
    @Qualifier('grailsDomainClassMappingContext')
    MappingContext mappingContext

    @Autowired(required = false)
    UrlConverter grailsUrlConverter = new CamelCaseUrlConverter()

    @Value('${grails.resources.pattern:/static/**}')
    String resourcePattern = Settings.DEFAULT_RESOURCE_PATTERN

    DefaultLinkGenerator(String serverBaseURL, String contextPath) {
        configuredServerBaseURL = serverBaseURL
        this.contextPath = contextPath
    }

    DefaultLinkGenerator(String serverBaseURL) {
        configuredServerBaseURL = serverBaseURL
    }

    @PostConstruct
    void initializeResourcePath() {
        if (resourcePattern?.endsWith('/**')) {
            resourcePath = resourcePattern.substring(0, resourcePattern.length() - 3)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String link(Map attrs, String encoding = 'UTF-8') {
        StringBuilder writer = new StringBuilder()
        // prefer URI attribute
        boolean includeContext = GrailsClassUtils.getBooleanFromMap(ATTRIBUTE_INCLUDE_CONTEXT, attrs, true)

        if (attrs.get(ATTRIBUTE_URI) != null) {
            String uri = attrs.get(ATTRIBUTE_URI)
            if (!isUriAbsolute(uri)) {
                String base = handleAbsolute(attrs)
                if (base != null) {
                    writer.append(base)
                }
                else if (includeContext) {
                    Object cp = attrs.get(ATTRIBUTE_CONTEXT_PATH)
                    if (cp == null) {
                        cp = getContextPath()
                    }
                    if (cp != null) {
                        writer.append(cp)
                    }
                }
            }
            writer.append(uri)

            Object params = attrs.get(ATTRIBUTE_PARAMS)

            if (params instanceof Map) {
                String charset = GrailsWebUtil.DEFAULT_ENCODING
                String paramString = params.collect { Map.Entry entry ->
                    String encodedKey = URLEncoder.encode(entry.key as String, charset)
                    String encodedValue = URLEncoder.encode(entry.value as String, charset)
                    "$encodedKey=$encodedValue"
                }.join('&')
                writer.append(uri.indexOf('?') >= 0 ? '&' : '?')
                        .append(paramString)
            }
        }
        else if (attrs.get(ATTRIBUTE_RELATIVE_URI) != null) {
            String relativeUri = attrs.get(ATTRIBUTE_RELATIVE_URI)
            String forwardUri = WebUtils.getForwardURI(requestStateLookupStrategy.webRequest.request)
            int index = forwardUri.lastIndexOf('/')
            if (index != -1) {
                writer.append forwardUri.substring(0, index + 1)
            }
            writer.append relativeUri
        }
        else {
            // prefer a URL attribute
            Map urlAttrs = attrs
            Object urlAttribute = attrs.get(ATTRIBUTE_URL)
            if (urlAttribute instanceof Map) {
                urlAttrs = (Map) urlAttribute
            }
            if (!urlAttribute || urlAttribute instanceof Map) {
                Object controllerAttribute = urlAttrs.get(ATTRIBUTE_CONTROLLER)
                Object resourceAttribute = urlAttrs.get(ATTRIBUTE_RESOURCE)
                String controller
                String action = urlAttrs.get(ATTRIBUTE_ACTION)
                Object id = urlAttrs.get(ATTRIBUTE_ID)
                String httpMethod
                Object methodAttribute = urlAttrs.get(ATTRIBUTE_METHOD)
                Object paramsAttribute = urlAttrs.get(ATTRIBUTE_PARAMS)
                Map params = (paramsAttribute instanceof Map) ? (Map) paramsAttribute : [:]

                if (resourceAttribute) {
                    String resource
                    if (resourceAttribute instanceof CharSequence) {
                        resource = resourceAttribute.toString()
                    }
                    else {
                        PersistentEntity persistentEntity = (mappingContext != null) ?
                                mappingContext.getPersistentEntity(resourceAttribute.getClass().getName()) : null
                        boolean hasId = false
                        if (persistentEntity != null) {
                            resource = persistentEntity.getDecapitalizedName()
                            hasId = true
                        }
                        else if (DomainClassArtefactHandler.isDomainClass(resourceAttribute.getClass(), true)) {
                            resource = GrailsNameUtils.getPropertyName(resourceAttribute.getClass())
                            hasId = true
                        }
                        else if (resourceAttribute instanceof Class) {
                            resource = GrailsNameUtils.getPropertyName(resourceAttribute)
                        }
                        else {
                            resource = resourceAttribute.toString()
                        }
                        if (!id && hasId) {
                            id = getResourceId(resourceAttribute)
                        }
                    }
                    List tokens = resource.contains('/') ? resource.tokenize('/') : [resource]
                    controller = tokens[-1]
                    if (tokens.size() > 1) {
                        for (t in tokens[0..-2]) {
                            String key = "${t}Id"
                            Object attr = urlAttrs.remove(key)
                            // the params value might not be null
                            // only overwrite if urlAttrs actually had the key
                            if (attr) {
                                params[key] = attr
                            }
                        }
                    }
                    if (!methodAttribute && action) {
                        httpMethod = REST_RESOURCE_ACTION_TO_HTTP_METHOD_MAP.get(action.toString())
                        httpMethod = httpMethod ?: HttpMethod.GET.toString()
                    }
                    else if (methodAttribute && !action) {
                        String method = methodAttribute.toString().toUpperCase()
                        httpMethod = method
                        if (method == 'GET' && id) {
                            method = "${method}_ID".toString()
                        }
                        action = REST_RESOURCE_HTTP_METHOD_TO_ACTION_MAP[method]
                    }
                    else {
                        httpMethod = methodAttribute == null ?
                                (requestStateLookupStrategy.getHttpMethod() ?: UrlMapping.ANY_HTTP_METHOD) : methodAttribute.toString()
                    }
                }
                else {
                    controller = controllerAttribute == null ? requestStateLookupStrategy.getControllerName() : controllerAttribute.toString()
                    httpMethod = methodAttribute == null ?
                            (requestStateLookupStrategy.getHttpMethod() ?: UrlMapping.ANY_HTTP_METHOD) : methodAttribute.toString()
                }

                String convertedControllerName = grailsUrlConverter.toUrlElement(controller)

                boolean isDefaultAction = false
                if (controller && !action) {
                    action = requestStateLookupStrategy.getActionName(convertedControllerName)
                    isDefaultAction = true
                }
                String convertedActionName = action
                if (action) {
                    convertedActionName = grailsUrlConverter.toUrlElement(action)
                }

                String frag = urlAttrs.get(ATTRIBUTE_FRAGMENT)

                Object mappingName = urlAttrs.get(ATTRIBUTE_MAPPING)
                if (mappingName != null) {
                    params.mappingName = mappingName
                }
                if (id != null) {
                    params.put(ATTRIBUTE_ID, id)
                }
                String pluginName = attrs.get(UrlMapping.PLUGIN)
                String namespace = attrs.get(UrlMapping.NAMESPACE)
                if (namespace == null) {
                    if (controller == requestStateLookupStrategy.controllerName) {
                        namespace = requestStateLookupStrategy.controllerNamespace
                    }
                }
                UrlCreator mapping = urlMappingsHolder.getReverseMappingNoDefault(controller, action, namespace, pluginName, httpMethod, params)
                if (mapping == null && isDefaultAction) {
                    mapping = urlMappingsHolder.getReverseMappingNoDefault(controller, null, namespace, pluginName, httpMethod, params)
                }
                if (mapping == null) {
                    mapping = urlMappingsHolder.getReverseMapping(controller, action, namespace, pluginName, httpMethod, params)
                }

                boolean absolute = isAbsolute(attrs)

                String url
                if (absolute) {
                    url = mapping.createRelativeURL(convertedControllerName, convertedActionName, namespace, pluginName, params, encoding, frag)
                    writer.append(handleAbsolute(attrs))
                    writer.append(url)
                }
                else {
                    url = mapping.createRelativeURL(convertedControllerName, convertedActionName, namespace, pluginName, params, encoding, frag)
                    Object contextPathAttribute = attrs.get(ATTRIBUTE_CONTEXT_PATH)
                    String cp = contextPathAttribute == null ? getContextPath() : contextPathAttribute.toString()
                    if (attrs.get(ATTRIBUTE_BASE) || cp == null) {
                        attrs.put(ATTRIBUTE_ABSOLUTE, true)
                        writer.append(handleAbsolute(attrs))
                    }
                    else if (includeContext) {
                        writer.append(cp)
                    }
                    writer.append(url)
                }
            }
            else {
                writer.append(urlAttribute)
            }
        }
        writer.toString()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected String getResourceId(Object resourceAttribute) {
        Object id = resourceAttribute?.id
        if (id) {
            return id.toString()
        }
        null
    }

    protected boolean isAbsolute(Map attrs) {
        boolean absolute = false
        Object o = attrs.get(ATTRIBUTE_ABSOLUTE)
        if (o instanceof Boolean) {
            absolute = o
        }
        else {
            if (o != null) {
                try {
                    String str = o
                    if (str) {
                        absolute = Boolean.parseBoolean(str)
                    }
                }
                catch (ignored) {
                }
            }
        }
        absolute
    }

    /**
     * {@inheritDoc }
     */
    @Override
    String resource(Map attrs) {
        String absolutePath = handleAbsolute(attrs)

        String contextPathAttribute = attrs.contextPath
        if (absolutePath == null) {
            String cp = contextPathAttribute == null ? getContextPath() : contextPathAttribute
            if (cp == null) {
                absolutePath = handleAbsolute(absolute: true)
            }
            else {
                absolutePath = cp
            }
        }

        StringBuilder url = new StringBuilder(absolutePath ?: '')
        String dir = attrs.dir
        if (attrs.plugin) {
            url.append(pluginManager?.getPluginPath(attrs.plugin?.toString()) ?: '')
        }
        else {
            if (contextPathAttribute == null) {
                String pluginContextPath = attrs.pluginContextPath
                if (pluginContextPath != null && dir != pluginContextPath) {
                    url << pluginContextPath
                }
            }
        }

        String slash = '/'
        if (resourcePath != null) {
            url.append(resourcePath)
        }
        if (dir) {
            if (!dir.startsWith(slash)) {
                url.append(slash)
            }
            url.append(dir)
        }

        String file = attrs.file
        if (file) {
            if (!(file.startsWith(slash) || (dir != null && dir.endsWith(slash)))) {
                url.append(slash)
            }
            url.append(file)
        }

        url.toString()
    }

    @Override
    String getContextPath() {
        if (contextPath == null) {
            contextPath = requestStateLookupStrategy.getContextPath()
        }
        contextPath
    }

    /**
     * Check for "absolute" attribute and render server URL if available from Config or deducible in non-production.
     */
    private String handleAbsolute(Map attrs) {
        Object base = attrs.base
        if (base) {
            return base.toString()
        }

        if (isAbsolute(attrs)) {
            String u = makeServerURL()
            if (u) {
                return u
            }

            throw new IllegalStateException("Attribute absolute='true' specified but no grails.serverURL set in Config")
        }
    }

    private boolean isUriAbsolute(String uri) {
        // not using new URI(uri).absolute in order to avoid create the URI object, which is slow
        ABSOLUTE_URL_PATTERN.matcher(uri).matches()
    }

    /**
     * Get the declared URL of the server from config, or guess at localhost for non-production.
     */
    String makeServerURL() {
        String u = configuredServerBaseURL
        if (!u) {
            // Leave it null if we're in production so we can throw
            GrailsWebRequest webRequest = GrailsWebRequest.lookup()

            u = webRequest?.baseUrl
            if (!u && !Environment.isWarDeployed()) {
                u = "http://localhost:${System.getProperty('server.port') ?: '8080'}${contextPath ?: ''}"
            }
        }
        log.trace("Resolved base server URL: $u")
        u
    }

    @Override
    String getServerBaseURL() {
        makeServerURL()
    }

    @Override
    void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
    }

}
