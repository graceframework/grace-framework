/*
 * Copyright 2006-2022 the original author or authors.
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
package org.grails.compiler.web.converters

import grails.artefact.Enhances
import org.grails.web.converters.Converter
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpServletResponse

@Enhances(["Controller", "Interceptor"])
trait RenderConverterTrait {

    /**
     * Render the given converter to the response
     *
     * @param converter The converter to render
     */
    void render(Converter<?> converter) {
        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()
        HttpServletResponse response = webRequest.currentResponse
        webRequest.renderView = false
        converter.render response
    }
}
