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
package org.grails.web.taglib.encoder;

import java.util.List;

import org.springframework.core.Ordered;

import org.grails.core.io.support.GrailsFactoriesLoader;
import org.grails.taglib.encoder.AbstractOutputContextLookup;
import org.grails.taglib.encoder.OutputContext;
import org.grails.taglib.encoder.OutputContextLookup;

/**
 * Web implementation for {@link OutputContextLookup}
 * Support customizing through {@link WebOutputContextInitializer}
 *
 * @author Lari Hotari
 * @since 3.0
 * @see WebOutputContext
 */
public class WebOutputContextLookup extends AbstractOutputContextLookup implements Ordered {

    protected final List<WebOutputContextInitializer> contextInitializers;

    public WebOutputContextLookup() {
        this.contextInitializers = GrailsFactoriesLoader.loadFactories(WebOutputContextInitializer.class);
    }

    @Override
    public OutputContext lookupOutputContext() {
        WebOutputContext webOutputContext = new WebOutputContext();
        initializeOutputContext(webOutputContext);
        customizeOutputContext(webOutputContext);
        return webOutputContext;
    }

    protected void initializeOutputContext(WebOutputContext webOutputContext) {
        for (WebOutputContextInitializer initializer : this.contextInitializers) {
            initializer.initialize(webOutputContext);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
