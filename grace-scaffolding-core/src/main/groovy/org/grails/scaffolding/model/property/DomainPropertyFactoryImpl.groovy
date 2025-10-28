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
package org.grails.scaffolding.model.property

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Value

import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * @see {@link DomainPropertyFactory}
 * @author James Kleeh
 * @since 2024.0.0
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
