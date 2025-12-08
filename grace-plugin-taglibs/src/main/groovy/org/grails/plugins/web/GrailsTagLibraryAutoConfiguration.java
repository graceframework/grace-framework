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
package org.grails.plugins.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import grails.core.GrailsApplication;

import org.grails.web.taglib.StandaloneTagLibraryLookup;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Grails Tag Library.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@AutoConfiguration
public class GrailsTagLibraryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultGrailsTagDateHelper grailsTagDateHelper() {
        return new DefaultGrailsTagDateHelper();
    }

    @Bean
    @ConditionalOnMissingBean
    public StandaloneTagLibraryLookup gspTagLibraryLookup(ObjectProvider<GrailsApplication> grailsApplication) {
        StandaloneTagLibraryLookup tagLibraryLookup = new StandaloneTagLibraryLookup();
        grailsApplication.ifAvailable(tagLibraryLookup::setGrailsApplication);
        return tagLibraryLookup;
    }

}
