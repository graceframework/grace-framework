/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplicationShutdownHookInstance;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

import grails.boot.config.GrailsAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests for {@link Grails}.
 */
@ExtendWith(OutputCaptureExtension.class)
public class GrailsTests {

    private String headlessProperty;

    private ConfigurableApplicationContext context;

    private Environment getEnvironment() {
        if (this.context != null) {
            return this.context.getEnvironment();
        }
        throw new IllegalStateException("Could not obtain Environment");
    }

    @BeforeEach
    void storeAndClearHeadlessProperty() {
        this.headlessProperty = System.getProperty("java.awt.headless");
        System.clearProperty("java.awt.headless");
    }

    @AfterEach
    void reinstateHeadlessProperty() {
        if (this.headlessProperty == null) {
            System.clearProperty("java.awt.headless");
        }
        else {
            System.setProperty("java.awt.headless", this.headlessProperty);
        }
    }

    @AfterEach
    void cleanUp() {
        if (this.context != null) {
            this.context.close();
        }
        System.clearProperty("spring.main.banner-mode");
        System.clearProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
        SpringApplicationShutdownHookInstance.reset();
    }

    @Test
    void sourcesMustNotBeNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Grails((Class<?>[]) null).run())
                .withMessageContaining("PrimarySources must not be null");
    }

    @Test
    void sourcesMustNotBeEmpty() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Grails().run())
                .withMessageContaining("Sources must not be empty");
    }

    @Test
    void logsActiveProfilesWithSingleDevelopment(CapturedOutput output) {
        Grails app = new Grails(ExampleConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        this.context = app.run();
        assertThat(output).contains("The following 1 profile is active: \"development\"");
    }

    @Test
    void bannerModeOnWithGrailsApp() {
        Grails app = new Grails(ExampleConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        this.context = app.run();
        assertThat(app).hasFieldOrPropertyWithValue("bannerMode", Banner.Mode.CONSOLE);
    }

    @Test
    void customId() {
        Grails app = new Grails(ExampleConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        this.context = app.run("--spring.application.name=foo");
        assertThat(this.context.getId()).startsWith("foo");
    }

    @Test
    void specificApplicationContextFactory() {
        Grails app = new Grails(ExampleConfig.class);
        app.setApplicationContextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class));
        this.context = app.run();
        assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
    }


    @Test
    void specificApplicationContextInitializer() {
        Grails app = new Grails(ExampleConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        final AtomicReference<ApplicationContext> reference = new AtomicReference<>();
        app.setInitializers(Collections
                .singletonList((ApplicationContextInitializer<ConfigurableApplicationContext>) reference::set));
        this.context = app.run("--foo=bar");
        assertThat(this.context).isSameAs(reference.get());
        // Custom initializers do not switch off the defaults
        assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
    }

    @Test
    void applicationRunningEventListener() {
        Grails app = new Grails(ExampleConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        AtomicReference<ApplicationReadyEvent> reference = addListener(app, ApplicationReadyEvent.class);
        this.context = app.run("--foo=bar");
        assertThat(app).isSameAs(reference.get().getSpringApplication());
    }

    @Test
    void customApplicationStartupPublishStartupSteps() {
        ApplicationStartup applicationStartup = mock(ApplicationStartup.class);
        StartupStep startupStep = mock(StartupStep.class);
        given(applicationStartup.start(anyString())).willReturn(startupStep);
        given(startupStep.tag(anyString(), anyString())).willReturn(startupStep);
        given(startupStep.tag(anyString(), ArgumentMatchers.<Supplier<String>>any())).willReturn(startupStep);
        Grails application = new Grails(GrailsAutoConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setApplicationStartup(applicationStartup);
        this.context = application.run();
        assertThat(this.context.getBean(ApplicationStartup.class)).isEqualTo(applicationStartup);
        then(applicationStartup).should().start("grails.application.config.prepared");
        then(applicationStartup).should().start("grails.application.artefact-classes.scan");
        then(applicationStartup).should().start("grails.application.artefact-classes.loaded");
        then(applicationStartup).should().start("grails.application.bean-definitions.registered");
        then(applicationStartup).should().start("grails.application.context.dynamic-methods");
        then(applicationStartup).should().start("grails.application.context.post-processing");
        then(applicationStartup).should().start("grails.application.context.startup");
        long startCount = mockingDetails(applicationStartup).getInvocations().stream()
                .filter((invocation) -> invocation.getMethod().toString().contains("start(")).count();
        long endCount = mockingDetails(startupStep).getInvocations().stream()
                .filter((invocation) -> invocation.getMethod().toString().contains("end(")).count();
        assertThat(startCount).isEqualTo(endCount);
    }

    private <E extends ApplicationEvent> AtomicReference<E> addListener(Grails app, Class<E> eventType) {
        AtomicReference<E> reference = new AtomicReference<>();
        app.addListeners(new TestEventListener<>(eventType, reference));
        return reference;
    }

    static class TestEventListener<E extends ApplicationEvent> implements SmartApplicationListener {

        private final Class<E> eventType;

        private final AtomicReference<E> reference;

        TestEventListener(Class<E> eventType, AtomicReference<E> reference) {
            this.eventType = eventType;
            this.reference = reference;
        }

        @Override
        public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
            return this.eventType.isAssignableFrom(eventType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onApplicationEvent(ApplicationEvent event) {
            this.reference.set((E) event);
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ExampleConfig {

        @Bean
        String someBean() {
            return "test";
        }

    }

}
