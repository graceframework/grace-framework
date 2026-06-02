/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.web.mapping.actuate.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import grails.gorm.validation.Constrained;
import grails.gorm.validation.ConstrainedProperty;
import grails.web.mapping.UrlMapping;
import grails.web.mapping.UrlMappingsHolder;

import org.grails.web.mapping.ResponseCodeMappingData;
import org.grails.web.mapping.ResponseCodeUrlMapping;

/**
 * {@link Endpoint @Endpoint} to expose details of a Grails application's url mappings.
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
@Endpoint(id = "urlmappings")
public class UrlMappingsEndpoint {

    UrlMappingsHolder grailsUrlMappingsHolder;

    public UrlMappingsEndpoint(UrlMappingsHolder grailsUrlMappingsHolder) {
        this.grailsUrlMappingsHolder = grailsUrlMappingsHolder;
    }

    @ReadOperation
    public UrlMappingsDescriptor urlMappings() {
        UrlMapping[] urlMappings = grailsUrlMappingsHolder.getUrlMappings();
        List<UrlMappingDescriptor> urlMappingDescriptors = new ArrayList<>();

        Stream.of(urlMappings).forEach(urlMapping -> {
            String urlPattern = establishUrlPattern(urlMapping);
            UrlMappingDescriptor urlMappingDescriptor = new UrlMappingDescriptor(
                    formatString(urlMapping.getNamespace()),
                    formatString(urlMapping.getControllerName()),
                    formatString(urlMapping.getActionName()),
                    formatString(urlMapping.getViewName()),
                    urlMapping.getHttpMethod(),
                    urlPattern,
                    urlMapping.getVersion(),
                    formatString(urlMapping.getPluginName())
            );
            urlMappingDescriptors.add(urlMappingDescriptor);
        });

        return new UrlMappingsDescriptor(urlMappingDescriptors);
    }

    protected String establishUrlPattern(UrlMapping urlMapping) {
        if (urlMapping instanceof ResponseCodeUrlMapping) {
            return String.valueOf(((ResponseCodeMappingData) urlMapping.getUrlData()).getResponseCode());
        }

        Constrained[] constraints = urlMapping.getConstraints();
        String[] tokens = urlMapping.getUrlData().getTokens();
        StringBuilder urlPattern = new StringBuilder(UrlMapping.SLASH);
        int constraintIndex = 0;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            boolean hasTokens = token.contains(UrlMapping.CAPTURED_WILDCARD) || token.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD);
            if (hasTokens) {
                String finalToken = token;
                while (hasTokens) {
                    if (finalToken.contains(UrlMapping.CAPTURED_WILDCARD)) {
                        ConstrainedProperty constraint = (ConstrainedProperty) constraints[constraintIndex++];
                        String prop = "\\${" + constraint.getPropertyName() + "}";
                        finalToken = finalToken.replaceFirst("(\\*)", prop);
                    }
                    else if (finalToken.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)) {
                        ConstrainedProperty constraint = (ConstrainedProperty) constraints[constraintIndex++];
                        String prop = "\\${" + constraint.getPropertyName() + "}**";
                        finalToken = finalToken.replaceFirst("(\\*\\*)", prop);
                    }
                    hasTokens = finalToken.contains(UrlMapping.CAPTURED_WILDCARD) || finalToken.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD);
                }
                urlPattern.append(finalToken);
            }
            else {
                urlPattern.append(token);
            }

            if (i < (tokens.length - 1)) {
                urlPattern.append(UrlMapping.SLASH);
            }
        }

        if (urlMapping.getUrlData().hasOptionalExtension()) {
            Constrained[] allConstraints = urlMapping.getConstraints();
            ConstrainedProperty lastConstraint = (ConstrainedProperty) allConstraints[allConstraints.length - 1];
            urlPattern.append("(.").append(lastConstraint.getPropertyName()).append(")?");
        }

        return urlPattern.toString();
    }

    private String formatString(Object value) {
        return value != null ? value.toString() : "";
    }

    public static final class UrlMappingsDescriptor {

        private final List<UrlMappingDescriptor> urlMappings;

        public UrlMappingsDescriptor(List<UrlMappingDescriptor> urlMappings) {
            this.urlMappings = urlMappings;
        }

        public List<UrlMappingDescriptor> getUrlMappings() {
            return this.urlMappings;
        }

    }

    public static final class UrlMappingDescriptor {

        private final String namespace;

        private final String controllerName;

        private final String actionName;

        private final String viewName;

        private final String httpMethod;

        private final String urlPattern;

        private final String version;

        private final String pluginName;

        public UrlMappingDescriptor(String namespace, String controllerName, String actionName, String viewName,
                String httpMethod, String urlPattern, String version, String pluginName) {
            this.namespace = namespace;
            this.controllerName = controllerName;
            this.actionName = actionName;
            this.viewName = viewName;
            this.httpMethod = httpMethod;
            this.urlPattern = urlPattern;
            this.version = version;
            this.pluginName = pluginName;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getControllerName() {
            return controllerName;
        }

        public String getActionName() {
            return actionName;
        }

        public String getViewName() {
            return viewName;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public String getUrlPattern() {
            return urlPattern;
        }

        public String getVersion() {
            return this.version;
        }

        public String getPluginName() {
            return pluginName;
        }

    }

}
