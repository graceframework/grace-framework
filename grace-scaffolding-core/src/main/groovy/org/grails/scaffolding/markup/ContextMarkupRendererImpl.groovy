/*
 * Copyright 2012-2025 the original author or authors.
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
package org.grails.scaffolding.markup

import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import org.springframework.context.MessageSource

import grails.util.GrailsNameUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.scaffolding.model.property.DomainProperty

/**
 * @see {@link ContextMarkupRenderer}
 * @author James Kleeh
 * @since 2024.0.0
 */
class ContextMarkupRendererImpl implements ContextMarkupRenderer {

    private MessageSource messageSource

    ContextMarkupRendererImpl(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    @CompileStatic
    protected String getDefaultTableHeader(DomainProperty property) {
        property.defaultLabel
    }

    @CompileStatic
    protected String getLabelText(DomainProperty property) {
        String labelText
        if (property.labelKeys) {
            labelText = resolveMessage(property.labelKeys, property.defaultLabel)
        }
        labelText ?: property.defaultLabel
    }

    @CompileStatic
    protected String resolveMessage(List<String> keysInPreferenceOrder, String defaultMessage) {
        String message = keysInPreferenceOrder.findResult { key ->
            this.messageSource.getMessage(key, [].toArray(), defaultMessage, Locale.default) ?: null
        }
        message ?: defaultMessage
    }

    @CompileStatic
    protected String toPropertyNameFormat(Class type) {
        String propertyNameFormat = GrailsNameUtils.getLogicalPropertyName(type.canonicalName, '')
        if (propertyNameFormat.endsWith('[]')) {
            propertyNameFormat = propertyNameFormat - '[]' + 'Array'
        }
        propertyNameFormat
    }

    @Override
    Closure<MarkupBuilder> listOutputContext(PersistentEntity domainClass, List<DomainProperty> properties, Closure content) {
        { ->
            table {
                thead {
                    tr {
                        properties.each {
                            th(getDefaultTableHeader(it))
                        }
                    }
                }
                tbody {
                    tr {
                        properties.each { property ->
                            td(content.call(property))
                        }
                    }
                }
            }
        }
    }

    @Override
    Closure<MarkupBuilder> inputContext(PersistentEntity domainClass, Closure content) {
        return { ->
            fieldset([class: 'form'], content)
        }
    }

    @Override
    Closure<MarkupBuilder> inputContext(DomainProperty property, Closure content) {
        List classes = ['fieldcontain']
        if (property.required) {
            classes << 'required'
        }
        return { ->
            content.delegate = delegate
            div(class: classes.join(' ')) {
                label([for: property.pathFromRoot], getLabelText(property)) {
                    if (property.required) {
                        span(class: 'required-indicator', '*')
                    }
                }
                content.call()
            }
        }
    }

    @Override
    Closure<MarkupBuilder> outputContext(PersistentEntity domainClass, Closure content) {
        return { ->
            ol([class: "property-list ${domainClass.decapitalizedName}"], content)
        }
    }

    @Override
    Closure<MarkupBuilder> outputContext(DomainProperty property, Closure content) {
        return { ->
            li(class: 'fieldcontain') {
                span([id: "${property.pathFromRoot}-label", class: 'property-label'], getLabelText(property))
                div([class: 'property-value', 'aria-labelledby': "${property.pathFromRoot}-label"], content)
            }
        }
    }

    @Override
    Closure<MarkupBuilder> embeddedOutputContext(DomainProperty property, Closure content) {
        embeddedInputContext(property, content)
    }

    @Override
    Closure<MarkupBuilder> embeddedInputContext(DomainProperty property, Closure content) {
        return { ->
            content.delegate = delegate
            fieldset(class: "embedded ${toPropertyNameFormat(property.type)}") {
                legend(getLabelText(property))
                content.call()
            }
        }
    }

}
