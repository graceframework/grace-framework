/*
 * Copyright 2015-2024 the original author or authors.
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
package grails.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ResourceLoader;

/**
 * Fluent API for constructing Grails instances.
 * Simple extension of {@link SpringApplicationBuilder}.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0.6
 */
public class GrailsBuilder extends SpringApplicationBuilder {

    public GrailsBuilder(Class<?>... sources) {
        super(sources);
    }

    @Override
    protected SpringApplication createSpringApplication(ResourceLoader resourceLoader, Class<?>... sources) {
        return new Grails(resourceLoader, sources);
    }

    @Override
    public Grails application() {
        return (Grails) super.application();
    }

    @Override
    public Grails build() {
        return build(new String[0]);
    }

    public Grails build(String... args) {
        return (Grails) super.build(args);
    }

}
