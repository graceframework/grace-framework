/*
 * Copyright 2013-2023 the original author or authors.
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
package org.grails.web.databinding

import org.springframework.core.convert.ConversionService as SpringConversionService
import org.springframework.core.convert.support.DefaultConversionService

import org.grails.databinding.converters.ConversionService

/**
 * This class implements {@link ConversionService}
 * and delegates to a {@link DefaultConversionService}.
 *
 * @see ConversionService
 * @see DefaultConversionService
 */
class SpringConversionServiceAdapter implements ConversionService {

    private final SpringConversionService springConversionService = new DefaultConversionService()

    boolean canConvert(Class<?> source, Class<?> target) {
        springConversionService.canConvert(source, target)
    }

    Object convert(Object object, Class<?> targetType) {
        springConversionService.convert(object, targetType)
    }

}
