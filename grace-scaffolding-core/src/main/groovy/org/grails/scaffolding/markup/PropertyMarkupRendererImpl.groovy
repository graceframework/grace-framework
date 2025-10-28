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

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.registry.DomainInputRendererRegistry
import org.grails.scaffolding.registry.DomainOutputRendererRegistry

/**
 * @see {@link PropertyMarkupRenderer}
 * @author James Kleeh
 * @since 2024.0.0
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
