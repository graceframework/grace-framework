/*
 * Copyright 2022-2023 the original author or authors.
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
package org.grails.taglib.encoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;

import org.grails.taglib.AbstractTemplateVariableBinding;
import org.grails.taglib.TemplateVariableBindingCustomizer;

/**
 * Abstract {@link OutputContext} to support customizing
 *
 * @author Michael Yan
 * @since 2022.0.0
 * @see TemplateVariableBindingCustomizer
 */
public abstract class AbstractOutputContext implements OutputContext {

    protected Set<TemplateVariableBindingCustomizer> variableBindingCustomizers = new LinkedHashSet<>();

    public void addVariableBindingCustomizer(TemplateVariableBindingCustomizer... customizers) {
        Assert.notNull(customizers, "Customizers must not be null");
        this.variableBindingCustomizers.addAll(Arrays.asList(customizers));
    }

    public void setVariableBindingCustomizers(Collection<? extends TemplateVariableBindingCustomizer> customizers) {
        Assert.notNull(customizers, "Customizers must not be null");
        this.variableBindingCustomizers = new LinkedHashSet<>(customizers);
    }

    public Collection<TemplateVariableBindingCustomizer> getVariableBindingCustomizers() {
        return this.variableBindingCustomizers;
    }

    protected void customizeTemplateVariableBinding(AbstractTemplateVariableBinding binding) {
        for (TemplateVariableBindingCustomizer customizer : getVariableBindingCustomizers()) {
            customizer.customize(binding);
        }
    }
}
