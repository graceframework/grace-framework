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

    static void callWithDelegate(delegate, Closure closure) {
        closure.delegate = delegate
        closure.call()
    }

    static String outputMarkupContent(Closure closure) {
        FastStringWriter writer = new FastStringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(writer)
        markupBuilder.doubleQuotes = true
        markupBuilder.escapeAttributes = false
        closure.delegate = markupBuilder
        if (closure.maximumNumberOfParameters == 1) {
            closure.call(markupBuilder)
        }
        else {
            closure.call()
        }
        writer.toString()
    }

    protected Closure renderInput(DomainProperty property) {
        this.contextMarkupRenderer.inputContext(property, this.propertyMarkupRenderer.renderInput(property))
    }

    protected Closure renderOutput(DomainProperty property) {
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
                this.domainModelService.getOutputProperties(((Embedded) property.persistentProperty).associatedEntity).each { DomainProperty embedded ->
                    embedded.rootProperty = property
                    tableProperties.add(embedded)
                }
            }
            else {
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
        outputMarkupContent(this.contextMarkupRenderer.inputContext(domainClass) { ->
            def contextDelegate = delegate
            this.domainModelService.getInputProperties(domainClass).each { DomainProperty property ->
                if (property.persistentProperty instanceof Embedded) {
                    callWithDelegate(contextDelegate, this.contextMarkupRenderer.embeddedInputContext(property) {
                        this.domainModelService.getInputProperties(((Embedded) property.persistentProperty).associatedEntity).each { DomainProperty embedded ->
                            embedded.rootProperty = property
                            callWithDelegate(contextDelegate, renderInput(embedded))
                        }
                    })
                }
                else {
                    callWithDelegate(contextDelegate, renderInput(property))
                }
            }
        }
        )
    }

    @Override
    String renderOutput(PersistentEntity domainClass) {
        outputMarkupContent(
                contextMarkupRenderer.outputContext(domainClass) { ->
                    def contextDelegate = delegate
                    domainModelService.getOutputProperties(domainClass).each { DomainProperty property ->
                        if (property.persistentProperty instanceof Embedded) {
                            callWithDelegate(contextDelegate, contextMarkupRenderer.embeddedOutputContext(property) { ->
                                domainModelService.getOutputProperties(((Embedded) property.persistentProperty).associatedEntity).each { DomainProperty embedded ->
                                    embedded.rootProperty = property
                                    callWithDelegate(contextDelegate, renderOutput(embedded))
                                }
                            })
                        }
                        else {
                            callWithDelegate(contextDelegate, renderOutput(property))
                        }
                    }
                }
        )
    }
}
