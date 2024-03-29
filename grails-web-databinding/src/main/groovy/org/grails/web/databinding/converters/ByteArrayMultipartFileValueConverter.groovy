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
package org.grails.web.databinding.converters

import groovy.transform.CompileStatic
import org.springframework.web.multipart.MultipartFile

import grails.databinding.converters.ValueConverter

/**
 * @author Jeff Brown
 * @since 2.3
 */
@CompileStatic
class ByteArrayMultipartFileValueConverter implements ValueConverter {

    @Override
    Object convert(Object value) {
        MultipartFile mf
        if (value instanceof MultipartFile) {
            mf = (MultipartFile) value
        }
        else if (value instanceof Collection) {
            Collection coll = (Collection) value
            if (coll.size() > 0) {
                Object firstElement = coll[0]
                if (firstElement instanceof MultipartFile) {
                    mf = (MultipartFile) firstElement
                }
            }
        }
        mf?.bytes
    }

    @Override
    Class<?> getTargetType() {
        byte[]
    }

    @Override
    boolean canConvert(Object value) {
        boolean canConvertValue = false
        if (value instanceof MultipartFile) {
            canConvertValue = true
        }
        else if (value instanceof Collection) {
            if (value.size() > 0 && ((Collection) value)[0] instanceof MultipartFile) {
                canConvertValue = true
            }
        }
        canConvertValue
    }

}
