/*
 * Copyright 2015-2023 the original author or authors.
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
package org.grails.web.mapping.servlet

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.ErrorPage
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.http.HttpStatus

import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappings

import org.grails.web.mapping.ResponseCodeMappingData
import org.grails.web.mapping.ResponseCodeUrlMapping

/**
 * Customizes the error pages based on UrlMappings
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class UrlMappingsErrorPageCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Autowired
    UrlMappings urlMappings

    @Override
    void customize(ConfigurableServletWebServerFactory container) {
        UrlMapping[] allMappings = urlMappings.getUrlMappings()

        List<ErrorPage> errorPages = []
        for (UrlMapping urlMapping : allMappings) {
            if (urlMapping instanceof ResponseCodeUrlMapping) {
                ResponseCodeUrlMapping responseCodeUrlMapping = (ResponseCodeUrlMapping) urlMapping
                ResponseCodeMappingData data = (ResponseCodeMappingData) responseCodeUrlMapping.urlData
                int code = data.responseCode
                errorPages << new ErrorPage(HttpStatus.valueOf(code), '/error')
            }
        }
        container.addErrorPages(errorPages as ErrorPage[])
    }

}
