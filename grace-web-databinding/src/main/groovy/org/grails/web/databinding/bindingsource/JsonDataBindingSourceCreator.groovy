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

import java.util.regex.Pattern

import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.web.mime.MimeType

import org.grails.databinding.bindingsource.DataBindingSourceCreationException
import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.grails.web.json.JSONObject

/**
 * Creates DataBindingSource objects from JSON in the request body
 *
 * @since 2.3
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @see DataBindingSource
 * @see DataBindingSourceCreator
 */
@CompileStatic
class JsonDataBindingSourceCreator extends AbstractRequestBodyDataBindingSourceCreator {

    private static final Pattern INDEX_PATTERN = ~/^(\S+)\[(\d+)\]$/

    @Autowired(required = false)
    JsonSlurper jsonSlurper = new JsonSlurper()

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        if (bindingSource instanceof Map) {
            return new SimpleMapDataBindingSource(createJsonMap(bindingSource))
        }
        else if (bindingSource instanceof JSONObject) {
            return new SimpleMapDataBindingSource((JSONObject) bindingSource)
        }

        super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {
        Object jsonElement = jsonSlurper.parse(reader)

        List<? extends DataBindingSource> dataBindingSources = jsonElement.collect { element ->
            if (element instanceof Map) {
                new SimpleMapDataBindingSource(createJsonMap(element))
            }
            else {
                new SimpleMapDataBindingSource(Collections.emptyMap())
            }
        }

        new CollectionDataBindingSource() {

            List<DataBindingSource> getDataBindingSources() {
                (List<DataBindingSource>) dataBindingSources
            }

        }
    }

    @Override
    protected DataBindingSource createBindingSource(Reader reader) {
        Object jsonElement = jsonSlurper.parse(reader)

        if (jsonElement instanceof Map) {
            return new SimpleMapDataBindingSource(createJsonMap(jsonElement))
        }

        new SimpleMapDataBindingSource(Collections.emptyMap())
    }

    protected Map createJsonMap(Object jsonElement) {
        (Map) jsonElement
    }

    @Override
    protected DataBindingSourceCreationException createBindingSourceCreationException(Exception e) {
        if (e instanceof JsonException) {
            return new InvalidRequestBodyException(e)
        }
        super.createBindingSourceCreationException(e)
    }

}