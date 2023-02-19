/*
 * Copyright 2018-2023 the original author or authors.
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
package org.grails.databinding.converters

import groovy.transform.CompileStatic

import grails.databinding.converters.ValueConverter

@CompileStatic
class UUIDConverter implements ValueConverter {

    @Override
    boolean canConvert(value) {
        value instanceof String
    }

    @Override
    Object convert(value) {
        if (value) {
            try {
                return UUID.fromString(value as String)
            }
            catch (IllegalArgumentException ignored) {
                return null
            }
        }
        null
    }

    @Override
    Class<?> getTargetType() {
        UUID
    }

}
