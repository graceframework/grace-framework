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

import java.util.Collection;

/**
 * Interface to lookup {@link OutputContext}
 *
 * @author Lari Hotari
 * @since 3.0
 */
public interface OutputContextLookup {

    OutputContext lookupOutputContext();

    void addContextCustomizers(OutputContextCustomizer... customizers);

    void setContextCustomizers(Collection<? extends OutputContextCustomizer> customizers);

    Collection<OutputContextCustomizer> getContextCustomizers();

}
