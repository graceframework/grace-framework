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
package org.grails.web.databinding.bindingsource

import jakarta.servlet.http.HttpServletRequest

import groovy.transform.CompileStatic

import grails.beans.util.LazyMetaPropertyMap
import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.web.databinding.DataBindingUtils
import grails.web.mime.MimeType
import grails.web.servlet.mvc.GrailsParameterMap

import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.grails.web.servlet.mvc.GrailsWebRequest

@CompileStatic
class DefaultDataBindingSourceCreator implements DataBindingSourceCreator {

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.ALL] as MimeType[]
    }

    @Override
    Class getTargetType() {
        Object
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        DataBindingSource dataBindingSource
        if (bindingSource instanceof DataBindingSource) {
            dataBindingSource = (DataBindingSource) bindingSource
        }
        else if (bindingSource instanceof HttpServletRequest) {
            dataBindingSource = createDataBindingSource(bindingTargetType, (HttpServletRequest) bindingSource)
        }
        else if (bindingSource instanceof Map) {
            dataBindingSource = new SimpleMapDataBindingSource(DataBindingUtils.convertPotentialGStrings((Map) bindingSource))
        }
        else if (bindingSource) {
            dataBindingSource = new SimpleMapDataBindingSource(new LazyMetaPropertyMap(bindingSource))
        }
        else {
            dataBindingSource = new SimpleMapDataBindingSource(Collections.emptyMap()) // LazyMetaPropertyMap dislike null source
        }
        dataBindingSource
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        throw new UnsupportedOperationException()
    }

    protected DataBindingSource createDataBindingSource(Object bindingTarget, HttpServletRequest req) {
        GrailsWebRequest grailsWebRequest = GrailsWebRequest.lookup(req)
        GrailsParameterMap parameterMap = grailsWebRequest.getParams()
        new SimpleMapDataBindingSource(parameterMap)
    }

}
