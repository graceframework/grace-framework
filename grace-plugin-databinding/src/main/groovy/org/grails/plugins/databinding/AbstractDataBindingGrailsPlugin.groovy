/*
 * Copyright 2013-2026 the original author or authors.
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
package org.grails.plugins.databinding

import grails.plugins.Plugin

/**
 * Plugin for configuring the data binding features of Grails
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @since 2.3
 */
abstract class AbstractDataBindingGrailsPlugin extends Plugin {

    public static final String DEFAULT_JSR310_OFFSET_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"
    public static final String DEFAULT_JSR310_OFFSET_TIME_FORMAT = 'HH:mm:ssZ'
    public static final String DEFAULT_JSR310_LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    public static final String DEFAULT_JSR310_LOCAL_DATE_FORMAT = 'yyyy-MM-dd'
    public static final String DEFAULT_JSR310_LOCAL_TIME_FORMAT = 'HH:mm:ss'
    public static final List<String> DEFAULT_DATE_FORMATS = [
            'yyyy-MM-dd HH:mm:ss.S',
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            'yyyy-MM-dd HH:mm:ss.S z',
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            DEFAULT_JSR310_OFFSET_ZONED_DATE_TIME_FORMAT,
            DEFAULT_JSR310_OFFSET_TIME_FORMAT,
            DEFAULT_JSR310_LOCAL_DATE_TIME_FORMAT,
            DEFAULT_JSR310_LOCAL_DATE_FORMAT,
            DEFAULT_JSR310_LOCAL_TIME_FORMAT]

}
