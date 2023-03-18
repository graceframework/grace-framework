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

import grails.core.GrailsApplication;

import org.grails.encoder.EncodingStateRegistry;
import org.grails.taglib.AbstractTemplateVariableBinding;

/**
 * Created by lari on 02/01/15.
 */
public interface OutputContext {

    EncodingStateRegistry getEncodingStateRegistry();

    OutputEncodingStack getCurrentOutputEncodingStack();

    void setCurrentOutputEncodingStack(OutputEncodingStack outputEncodingStack);

    Writer getCurrentWriter();

    void setCurrentWriter(Writer writer);

    AbstractTemplateVariableBinding createAndRegisterRootBinding();

    AbstractTemplateVariableBinding getBinding();

    void setBinding(AbstractTemplateVariableBinding binding);

    GrailsApplication getGrailsApplication();

    void setContentType(String contentType);

    boolean isContentTypeAlreadySet();

}
