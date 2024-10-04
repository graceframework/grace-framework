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
package org.grails.plugins.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockServletContext;

import grails.core.DefaultGrailsApplication;

import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator;
import org.grails.spring.aop.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroovyAopAutoConfiguration}.
 *
 * @author Michael Yan
 */
public class GroovyAopAutoConfigurationTests {

    private final AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();

    @BeforeEach
    void setupContext() {
        this.context.setServletContext(new MockServletContext());
    }

    @AfterEach
    void close() {
        this.context.close();
    }

    @Test
    void defaultConfiguration() {
        registerAndRefreshContext();
        assertThat(this.context.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)).isNotNull();
        assertThat(this.context.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)).isInstanceOf(GroovyAwareAspectJAwareAdvisorAutoProxyCreator.class);
    }

    @Test
    void disableAspectjAutoweave() {
        TestPropertyValues.of("grails.spring.disable.aspectj.autoweaving:true").applyTo(this.context);
        registerAndRefreshContext();
        assertThat(this.context.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)).isNotNull();
        assertThat(this.context.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)).isInstanceOf(GroovyAwareInfrastructureAdvisorAutoProxyCreator.class);
    }

    private void registerAndRefreshContext(String... env) {
        TestPropertyValues.of(env).applyTo(this.context);
        this.context.registerBean("grailsApplication", DefaultGrailsApplication.class);
        this.context.register(GroovyAopAutoConfiguration.class);
        this.context.refresh();
    }

}
