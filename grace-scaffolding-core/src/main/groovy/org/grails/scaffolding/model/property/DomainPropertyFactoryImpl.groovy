package org.grails.scaffolding.model.property

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty
import org.springframework.beans.factory.annotation.Value

/**
 * @see {@link DomainPropertyFactory}
 * @author James Kleeh
 */
@CompileStatic
class DomainPropertyFactoryImpl implements DomainPropertyFactory {

    @Value('${grails.databinding.convertEmptyStringsToNull:true}')
    Boolean convertEmptyStringsToNull

    @Value('${grails.databinding.trimStrings:true}')
    Boolean trimStrings

    private MappingContext grailsDomainClassMappingContext

    DomainPropertyFactoryImpl(MappingContext grailsDomainClassMappingContext) {
        this.grailsDomainClassMappingContext = grailsDomainClassMappingContext
    }

    @Override
    DomainProperty build(PersistentProperty persistentProperty) {
        DomainPropertyImpl domainProperty = new DomainPropertyImpl(persistentProperty, this.grailsDomainClassMappingContext)
        init(domainProperty)
        domainProperty
    }

    @Override
    DomainProperty build(PersistentProperty rootProperty, PersistentProperty persistentProperty) {
        DomainPropertyImpl domainProperty = new DomainPropertyImpl(rootProperty, persistentProperty, this.grailsDomainClassMappingContext)
        init(domainProperty)
        domainProperty
    }

    private init(DomainPropertyImpl domainProperty) {
        domainProperty.convertEmptyStringsToNull = this.convertEmptyStringsToNull
        domainProperty.trimStrings = this.trimStrings
    }
}
