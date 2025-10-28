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
package org.grails.scaffolding.registry;

import java.util.Map;

import groovy.lang.Closure;
import groovy.xml.MarkupBuilder;

import org.grails.scaffolding.model.property.DomainProperty;

/**
 * Used to render a single domain class property on a form
 *
 * @author James Kleeh
 * @since 2024.0.0
 */
public interface DomainInputRenderer extends DomainRenderer {

    /**
     * Defines how a given domain class property will be rendered in the context of a form
     *
     * @param defaultAttributes The default html element attributes
     * @param property          The domain property to be rendered
     * @return The closure to be passed to an instance of {@link MarkupBuilder}
     */
    Closure renderInput(Map defaultAttributes, DomainProperty property);

}
