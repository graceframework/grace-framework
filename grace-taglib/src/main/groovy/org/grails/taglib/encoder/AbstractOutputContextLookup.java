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

/**
 * Abstract {@link OutputContextLookup} to support customizing
 *
 * @author Michael Yan
 * @since 2022.0.0
 * @see OutputContextCustomizer
 */
public abstract class AbstractOutputContextLookup implements OutputContextLookup {

    protected Set<OutputContextCustomizer> customizers = new LinkedHashSet<>();

    public void addContextCustomizers(OutputContextCustomizer... customizers) {
        Assert.notNull(customizers, "Customizers must not be null");
        this.customizers.addAll(Arrays.asList(customizers));
    }

    public void setContextCustomizers(Collection<? extends OutputContextCustomizer> customizers) {
        Assert.notNull(customizers, "Customizers must not be null");
        this.customizers = new LinkedHashSet<>(customizers);
    }

    public Collection<OutputContextCustomizer> getContextCustomizers() {
        return this.customizers;
    }

    protected void customizeOutputContext(OutputContext outputContext) {
        for (OutputContextCustomizer customizer : getContextCustomizers()) {
            customizer.customize(outputContext);
        }
    }

}
