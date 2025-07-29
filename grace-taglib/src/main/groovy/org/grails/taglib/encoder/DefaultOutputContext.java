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

import java.io.Writer;

import grails.core.GrailsApplication;
import grails.util.Holders;

import org.grails.encoder.DefaultEncodingStateRegistry;
import org.grails.encoder.EncodingStateRegistry;
import org.grails.taglib.AbstractTemplateVariableBinding;
import org.grails.taglib.TemplateVariableBinding;

/**
 * Default implementation for {@link OutputContext}
 *
 * @author Lari Hotari
 * @since 3.0
 */
public class DefaultOutputContext extends AbstractOutputContext {

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
        customizeTemplateVariableBinding(this.binding);
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
