/*
 * Copyright 2024 the original author or authors.
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
package org.grails.plugins.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import grails.core.DefaultGrailsApplication;

import org.grails.datastore.mapping.model.MappingContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DomainClassAutoConfiguration}.
 *
 * @author Michael Yan
 */
public class DomainClassAutoConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @BeforeEach
    void setupContext() {
    }

    @AfterEach
    void close() {
        this.context.close();
    }

    @Test
    void defaultConfiguration() {
        registerAndRefreshContext();
        assertThat(this.context.getBean(MappingContext.class)).isNotNull();
    }

    private void registerAndRefreshContext(String... env) {
        TestPropertyValues.of(env).applyTo(this.context);
        this.context.registerBean("grailsApplication", TestGrailsApplication.class, this.context);
        this.context.registerBean("messageSource", StaticMessageSource.class);
        this.context.register(DomainClassAutoConfiguration.class);
        this.context.refresh();
    }

    static class TestGrailsApplication extends DefaultGrailsApplication {
        public TestGrailsApplication(ApplicationContext parentCtx) {
            super.setApplicationContext(parentCtx);
        }
    }

}
