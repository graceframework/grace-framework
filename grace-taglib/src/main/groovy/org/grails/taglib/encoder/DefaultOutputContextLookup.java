/*
 * Copyright 2015-2023 the original author or authors.
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

import org.springframework.core.Ordered;

import org.grails.encoder.EncodingStateRegistry;
import org.grails.encoder.EncodingStateRegistryLookup;
import org.grails.encoder.EncodingStateRegistryLookupHolder;

/**
 * Default implementation for {@link OutputContextLookup}
 *
 * @author Lari Hotari
 * @since 3.0
 */
public class DefaultOutputContextLookup extends AbstractOutputContextLookup implements EncodingStateRegistryLookup, Ordered {

    private ThreadLocal<OutputContext> outputContextThreadLocal = new ThreadLocal<OutputContext>() {
        @Override
        protected OutputContext initialValue() {
            return new DefaultOutputContext();
        }
    };

    public DefaultOutputContextLookup() {

    }

    @Override
    public OutputContext lookupOutputContext() {
        if (EncodingStateRegistryLookupHolder.getEncodingStateRegistryLookup() == null) {
            // TODO: improve EncodingStateRegistry solution so that global state doesn't have to be used
            EncodingStateRegistryLookupHolder.setEncodingStateRegistryLookup(this);
        }
        OutputContext outputContext = this.outputContextThreadLocal.get();
        customizeOutputContext(outputContext);
        return outputContext;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public EncodingStateRegistry lookup() {
        return lookupOutputContext().getEncodingStateRegistry();
    }

}
