/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.plugins.codecs

import org.springframework.core.PriorityOrdered

import grails.plugins.Plugin
import grails.util.GrailsUtil

import org.grails.encoder.impl.HTML4Codec
import org.grails.encoder.impl.HTMLJSCodec
import org.grails.encoder.impl.JavaScriptCodec
import org.grails.encoder.impl.RawCodec

/**
 * Configures pluggable codecs.
 *
 * @author Jeff Brown
 * @since 0.4
 * @deprecated as of 2022.0.0; use {@link CodecsPluginConfiguration} instead
 */
class CodecsGrailsPlugin extends Plugin implements PriorityOrdered {

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version]
    def watchedResources = ['file:./grails-app/utils/**/*Codec.groovy', 'file:./app/utils/**/*Codec.groovy']
    def providedArtefacts = [
            HTMLCodec,
            HTML4Codec,
            JavaScriptCodec,
            HTMLJSCodec,
            URLCodec,
            RawCodec
    ]

    Closure doWithSpring() {
        { ->
            // Keep this because it is used by testing-support
        }
    }

    @Override
    int getOrder() {
        30
    }

}
