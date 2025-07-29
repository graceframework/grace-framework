/*
 * Copyright 2024 the original author or authors.
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
package org.grails.plugins.converters;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import grails.converters.JSON;
import org.grails.web.converters.configuration.ObjectMarshallerRegisterer;
import org.grails.web.converters.marshaller.json.ValidationErrorsMarshaller;

/**
 * {@link EnableAutoConfiguration Auto-configure} for Json Converters
 *
 * @author Michael Yan
 * @since 2023.1
 */
@Configuration(proxyBeanMethods = false)
public class JsonConvertersConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "jsonErrorsMarshaller")
    public ValidationErrorsMarshaller jsonErrorsMarshaller() {
        return new ValidationErrorsMarshaller();
    }

    @Bean
    @ConditionalOnMissingBean(name = "errorsJsonMarshallerRegisterer")
    public ObjectMarshallerRegisterer errorsJsonMarshallerRegisterer(ValidationErrorsMarshaller jsonErrorsMarshaller) {
        ObjectMarshallerRegisterer objectMarshallerRegisterer = new ObjectMarshallerRegisterer();
        objectMarshallerRegisterer.setMarshaller(jsonErrorsMarshaller);
        objectMarshallerRegisterer.setConverterClass(JSON.class);
        return objectMarshallerRegisterer;
    }

}
