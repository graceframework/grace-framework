/*
 * Copyright 2015-2022 the original author or authors.
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

import java.io.Writer;

import org.springframework.core.Ordered;

import grails.core.GrailsApplication;
import grails.util.Holders;

import org.grails.encoder.DefaultEncodingStateRegistry;
import org.grails.encoder.EncodingStateRegistry;
import org.grails.encoder.EncodingStateRegistryLookup;
import org.grails.encoder.EncodingStateRegistryLookupHolder;
import org.grails.taglib.AbstractTemplateVariableBinding;
import org.grails.taglib.TemplateVariableBinding;

public class DefaultOutputContextLookup implements OutputContextLookup, EncodingStateRegistryLookup, Ordered {

    private ThreadLocal<OutputContext> outputContextThreadLocal = new ThreadLocal<OutputContext>() {
        @Override
        protected OutputContext initialValue() {
            return new DefaultOutputContext();
        }
    };

    @Override
    public OutputContext lookupOutputContext() {
        if (EncodingStateRegistryLookupHolder.getEncodingStateRegistryLookup() == null) {
            // TODO: improve EncodingStateRegistry solution so that global state doesn't have to be used
            EncodingStateRegistryLookupHolder.setEncodingStateRegistryLookup(this);
        }
        return this.outputContextThreadLocal.get();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public EncodingStateRegistry lookup() {
        return lookupOutputContext().getEncodingStateRegistry();
    }

    public DefaultOutputContextLookup() {

    }

    public static class DefaultOutputContext implements OutputContext {

        private OutputEncodingStack outputEncodingStack;

        private Writer currentWriter;

        private AbstractTemplateVariableBinding binding;

        private EncodingStateRegistry encodingStateRegistry = new DefaultEncodingStateRegistry();

        public DefaultOutputContext() {

        }

        @Override
        public EncodingStateRegistry getEncodingStateRegistry() {
            return this.encodingStateRegistry;
        }

        @Override
        public void setCurrentOutputEncodingStack(OutputEncodingStack outputEncodingStack) {
            this.outputEncodingStack = outputEncodingStack;
        }

        @Override
        public OutputEncodingStack getCurrentOutputEncodingStack() {
            return this.outputEncodingStack;
        }

        @Override
        public Writer getCurrentWriter() {
            return this.currentWriter;
        }

        @Override
        public void setCurrentWriter(Writer currentWriter) {
            this.currentWriter = currentWriter;
        }

        @Override
        public AbstractTemplateVariableBinding createAndRegisterRootBinding() {
            this.binding = new TemplateVariableBinding();
            return this.binding;
        }

        @Override
        public AbstractTemplateVariableBinding getBinding() {
            return this.binding;
        }

        @Override
        public void setBinding(AbstractTemplateVariableBinding binding) {
            this.binding = binding;
        }

        @Override
        public GrailsApplication getGrailsApplication() {
            return Holders.findApplication();
        }

        @Override
        public void setContentType(String contentType) {
            // no-op
        }

        @Override
        public boolean isContentTypeAlreadySet() {
            return true;
        }

    }

}
