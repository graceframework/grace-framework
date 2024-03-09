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
package grails.web.mapping

import groovy.transform.CompileStatic
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter

import org.grails.web.mapping.DefaultLinkGenerator

/**
 * Helper class for creating a {@link LinkGenerator}. Useful for testing
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class LinkGeneratorFactory implements ApplicationContextAware {

    UrlMappingsFactory urlMappingsFactory = new UrlMappingsFactory()
    UrlConverter urlConverter = new CamelCaseUrlConverter()
    String baseURL = 'http://localhost'
    String contextPath = null

    LinkGenerator create(Class mappings) {
        UrlMappings urlMappings = urlMappingsFactory.create(mappings)
        create(urlMappings)
    }

    LinkGenerator create(Closure mappings) {
        UrlMappings urlMappings = urlMappingsFactory.create(mappings)
        create(urlMappings)
    }

    LinkGenerator create(UrlMappings urlMappings) {
        DefaultLinkGenerator generator = new DefaultLinkGenerator(baseURL, contextPath)
        generator.grailsUrlConverter = urlConverter
        generator.urlMappingsHolder = (UrlMappingsHolder) urlMappings
        generator
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        urlMappingsFactory.applicationContext = applicationContext
    }

}
