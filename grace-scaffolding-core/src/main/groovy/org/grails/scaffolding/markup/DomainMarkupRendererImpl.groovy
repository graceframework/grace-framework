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

import org.grails.buffer.FastStringWriter
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.scaffolding.model.DomainModelService
import org.grails.scaffolding.model.property.DomainProperty

/**
 * @see {@link DomainMarkupRenderer}
 * @author James Kleeh
 * @since 2024.0.0
 */
@CompileStatic
class DomainMarkupRendererImpl implements DomainMarkupRenderer {

    private DomainModelService domainModelService

    private PropertyMarkupRenderer propertyMarkupRenderer

    private ContextMarkupRenderer contextMarkupRenderer

    DomainMarkupRendererImpl(DomainModelService domainModelService, PropertyMarkupRenderer propertyMarkupRenderer,
                             ContextMarkupRenderer contextMarkupRenderer) {
        this.domainModelService = domainModelService
        this.propertyMarkupRenderer = propertyMarkupRenderer
        this.contextMarkupRenderer = contextMarkupRenderer
    }

    static void callWithDelegate(Object delegate, Closure<?> closure) {
        closure.delegate = delegate
        closure.call()
    }

    static String outputMarkupContent(Closure<MarkupBuilder> closure) {
        FastStringWriter writer = new FastStringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(writer)
        markupBuilder.doubleQuotes = true
        markupBuilder.escapeAttributes = false
        closure.delegate = markupBuilder
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        if (closure.maximumNumberOfParameters == 1) {
            closure.call(markupBuilder)
        } else {
            closure.call()
        }
        writer.toString()
    }

    protected Closure<MarkupBuilder> renderInput(DomainProperty property) {
        this.contextMarkupRenderer.inputContext(property, this.propertyMarkupRenderer.renderInput(property))
    }

    protected Closure<MarkupBuilder> renderOutput(DomainProperty property) {
        this.contextMarkupRenderer.outputContext(property, this.propertyMarkupRenderer.renderOutput(property))
    }

    /**
     * Determines how many properties will be included in the list output
     */
    protected int getMaxListOutputSize() {
        7
    }

    @Override
    String renderListOutput(PersistentEntity domainClass) {
        List<DomainProperty> tableProperties = []
        List<DomainProperty> domainProperties = this.domainModelService.getListOutputProperties(domainClass)
        domainProperties.each { DomainProperty property ->
            if (property.persistentProperty instanceof Embedded) {
                PersistentEntity associatedEntity = ((Embedded) property.persistentProperty).associatedEntity
                this.domainModelService.getOutputProperties(associatedEntity).each { DomainProperty embedded ->
                    embedded.rootProperty = property
                    tableProperties.add(embedded)
                }
            } else {
                tableProperties.add(property)
            }
        }
        if (tableProperties.size() > maxListOutputSize) {
            tableProperties = tableProperties[0..(maxListOutputSize - 1)]
        }
        outputMarkupContent(
                this.contextMarkupRenderer.listOutputContext(domainClass, tableProperties) { DomainProperty domainProperty ->
                    this.propertyMarkupRenderer.renderListOutput(domainProperty)
                }
        )
    }

    @Override
    String renderInput(PersistentEntity domainClass) {
        outputMarkupContent(this.contextMarkupRenderer.inputContext(domainClass) { Closure c ->
            // def contextDelegate = delegate
            this.domainModelService.getInputProperties(domainClass).each { DomainProperty property ->
                if (property.persistentProperty instanceof Embedded) {
                    callWithDelegate(delegate, this.contextMarkupRenderer.embeddedInputContext(property) {
                        PersistentEntity associatedEntity = ((Embedded) property.persistentProperty).associatedEntity
                        this.domainModelService.getInputProperties(associatedEntity).each { DomainProperty embedded ->
                            embedded.rootProperty = property
                            callWithDelegate(delegate, renderInput(embedded))
                        }
                    })
                } else {
                    callWithDelegate(delegate, renderInput(property))
                }
            }
        })
    }

    @Override
    String renderOutput(PersistentEntity domainClass) {
        outputMarkupContent(this.contextMarkupRenderer.outputContext(domainClass) { Closure c ->
            // def contextDelegate = delegate
            this.domainModelService.getOutputProperties(domainClass).each { DomainProperty property ->
                if (property.persistentProperty instanceof Embedded) {
                    callWithDelegate(delegate, this.contextMarkupRenderer.embeddedOutputContext(property) { ->
                        PersistentEntity associatedEntity = ((Embedded) property.persistentProperty).associatedEntity
                        this.domainModelService.getOutputProperties(associatedEntity).each { DomainProperty embedded ->
                            embedded.rootProperty = property
                            callWithDelegate(delegate, renderOutput(embedded))
                        }
                    })
                } else {
                    callWithDelegate(delegate, renderOutput(property))
                }
            }
        })
    }

}
