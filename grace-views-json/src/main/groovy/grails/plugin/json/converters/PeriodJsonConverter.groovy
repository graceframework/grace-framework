/*
 * Copyright 2018-2025 the original author or authors.
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
package grails.plugin.json.converters

import java.time.Period

import groovy.json.JsonGenerator
import groovy.transform.CompileStatic

/**
 * A class to render a {@link Period} as json
 *
 * @author Muhammad Hamza Zaib
 * @since 2024.0.0
 */
@CompileStatic
class PeriodJsonConverter implements JsonGenerator.Converter {

    @Override
    boolean handles(Class<?> type) {
        Period == type
    }

    @Override
    Object convert(Object value, String key) {
        ((Period) value).toString()
    }

}
