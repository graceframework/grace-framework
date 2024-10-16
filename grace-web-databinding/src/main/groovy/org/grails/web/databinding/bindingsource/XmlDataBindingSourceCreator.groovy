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

import groovy.transform.CompileStatic
import groovy.xml.slurpersupport.GPathResult
import org.xml.sax.SAXParseException

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.web.mime.MimeType

import org.grails.databinding.bindingsource.DataBindingSourceCreationException
import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.grails.databinding.xml.GPathResultCollectionDataBindingSource
import org.grails.databinding.xml.GPathResultMap
import org.grails.io.support.SpringIOUtils

/**
 * Creates DataBindingSource objects from XML in the request body
 *
 * @since 2.3
 * @see DataBindingSource
 * @see DataBindingSourceCreator
 */
@CompileStatic
class XmlDataBindingSourceCreator extends AbstractRequestBodyDataBindingSourceCreator {

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.XML, MimeType.TEXT_XML] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        if (bindingSource instanceof GPathResult) {
            GPathResultMap gpathMap = new GPathResultMap((GPathResult) bindingSource)
            return new SimpleMapDataBindingSource(gpathMap)
        }
        super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    protected DataBindingSource createBindingSource(Reader reader) {
        GPathResult gpath = SpringIOUtils.createXmlSlurper().parse(reader)
        GPathResultMap gpathMap = new GPathResultMap(gpath)
        new SimpleMapDataBindingSource(gpathMap)
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        if (bindingSource instanceof GPathResult) {
            new GPathResultCollectionDataBindingSource(bindingSource)
        }

        super.createCollectionDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {
        GPathResult gpath = SpringIOUtils.createXmlSlurper().parse(reader)
        new GPathResultCollectionDataBindingSource(gpath)
    }

    @Override
    protected DataBindingSourceCreationException createBindingSourceCreationException(Exception e) {
        if (e instanceof SAXParseException) {
            return new InvalidRequestBodyException(e)
        }
        super.createBindingSourceCreationException(e)
    }

}
