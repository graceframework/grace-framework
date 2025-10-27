package org.grails.scaffolding.markup

import groovy.transform.CompileStatic

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.registry.DomainInputRendererRegistry
import org.grails.scaffolding.registry.DomainOutputRendererRegistry

/**
 * @see {@link PropertyMarkupRenderer}
 * @author James Kleeh
 */
@CompileStatic
class PropertyMarkupRendererImpl implements PropertyMarkupRenderer {

    DomainInputRendererRegistry domainInputRendererRegistry

    DomainOutputRendererRegistry domainOutputRendererRegistry

    PropertyMarkupRendererImpl() {
    }

    PropertyMarkupRendererImpl(DomainInputRendererRegistry domainInputRendererRegistry, DomainOutputRendererRegistry domainOutputRendererRegistry) {
        this.domainInputRendererRegistry = domainInputRendererRegistry
        this.domainOutputRendererRegistry = domainOutputRendererRegistry
    }

    @Override
    Closure renderListOutput(DomainProperty property) {
        this.domainOutputRendererRegistry.get(property).renderListOutput(property)
    }

    @Override
    Closure renderOutput(DomainProperty property) {
        this.domainOutputRendererRegistry.get(property).renderOutput(property)
    }

    @Override
    Closure renderInput(DomainProperty property) {
        this.domainInputRendererRegistry.get(property).renderInput(getStandardAttributes(property), property)
    }

}
