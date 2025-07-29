/*
 * Copyright 2013-2024 the original author or authors.
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
package org.grails.web.databinding.bindingsource

import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest

import groovy.transform.CompileStatic
import org.springframework.http.HttpMethod

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.web.mime.MimeType
import grails.web.servlet.mvc.GrailsParameterMap

import org.grails.databinding.bindingsource.DataBindingSourceCreationException

@CompileStatic
abstract class AbstractRequestBodyDataBindingSourceCreator extends DefaultDataBindingSourceCreator {

    List<HttpMethod> ignoredRequestBodyMethods = [HttpMethod.GET, HttpMethod.DELETE]

    @Override
    Class getTargetType() {
        Object
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource)
            throws DataBindingSourceCreationException {
        try {
            if (bindingSource instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) bindingSource
                HttpMethod method = HttpMethod.valueOf(req.method)
                if (req.contentLength != 0 && !ignoredRequestBodyMethods.contains(method)) {
                    ServletInputStream is = req.getInputStream()
                    return createBindingSource(is, req.getCharacterEncoding())
                }
            }
            if (bindingSource instanceof InputStream) {
                InputStream is = (InputStream) bindingSource
                return createBindingSource(is, 'UTF-8')
            }
            if (bindingSource instanceof Reader) {
                Reader is = (Reader) bindingSource
                return createBindingSource(is)
            }

            return super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
        }
        catch (Exception e) {
            throw createBindingSourceCreationException(e)
        }
    }

    protected DataBindingSourceCreationException createBindingSourceCreationException(Exception e) {
        new DataBindingSourceCreationException(e)
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource)
            throws DataBindingSourceCreationException {
        try {
            if (bindingSource instanceof GrailsParameterMap) {
                HttpServletRequest req = bindingSource.getRequest()
                ServletInputStream is = req.getInputStream()
                return createCollectionBindingSource(is, req.getCharacterEncoding())
            }
            if (bindingSource instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) bindingSource
                ServletInputStream is = req.getInputStream()
                return createCollectionBindingSource(is, req.getCharacterEncoding())
            }
            if (bindingSource instanceof InputStream) {
                InputStream is = (InputStream) bindingSource
                return createCollectionBindingSource(is, 'UTF-8')
            }
            if (bindingSource instanceof Reader) {
                Reader is = (Reader) bindingSource
                return createCollectionBindingSource(is)
            }

            return super.createCollectionDataBindingSource(mimeType, bindingTargetType, bindingSource)
        }
        catch (Exception e) {
            throw new DataBindingSourceCreationException(e)
        }
    }

    protected DataBindingSource createBindingSource(InputStream inputStream, String charsetName) {
        createBindingSource(new InputStreamReader(inputStream, charsetName ?: 'UTF-8'))
    }

    protected abstract DataBindingSource createBindingSource(Reader reader)

    protected CollectionDataBindingSource createCollectionBindingSource(InputStream inputStream, String charsetName) {
        createCollectionBindingSource(new InputStreamReader(inputStream, charsetName ?: 'UTF-8'))
    }

    protected abstract CollectionDataBindingSource createCollectionBindingSource(Reader reader)

}
