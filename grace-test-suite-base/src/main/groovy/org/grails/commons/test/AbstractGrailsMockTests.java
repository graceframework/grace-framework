/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.commons.test;

import java.io.IOException;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.test.GroovyTestCase;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.ClassUtils;

import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;

import org.grails.config.PropertySourcesConfig;
import org.grails.support.MockApplicationContext;

/**
 * Abstract simple test harness for testing Grails Applications that just loads
 * the parsed classes into the GrailsApplication instance.
 *
 * @author Graeme Rocher
 */
public abstract class AbstractGrailsMockTests extends GroovyTestCase {

    /**
     * A GroovyClassLoader instance
     */
    public GroovyClassLoader gcl = new GroovyClassLoader();

    /**
     * The GrailsApplication instance created during setup
     */
    public DefaultGrailsApplication ga;

    public MockApplicationContext ctx;

    @Override
    @SuppressWarnings("unchecked")
    protected final void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally();
        super.setUp();

        System.out.println("Setting up test");
        this.ctx = new MockApplicationContext();
        this.ctx.registerMockBean(GrailsApplication.CLASS_LOADER_BEAN, this.gcl);
        onSetUp();
        this.ga = new DefaultGrailsApplication(this.gcl.getLoadedClasses(), this.gcl);
        if (ClassUtils.isPresent("Config", this.gcl)) {
            ConfigObject config = new ConfigSlurper().parse(this.gcl.loadClass("Config"));
            this.ga.setConfig(new PropertySourcesConfig(config));
        }
        this.ga.setApplicationContext(this.ctx);
        this.ga.initialise();
        this.ctx.registerMockBean(GrailsApplication.APPLICATION_ID, this.ga);
        postSetUp();
    }

    @Override
    protected final void tearDown() throws Exception {
        onTearDown();
        ExpandoMetaClass.disableGlobally();
        super.tearDown();
    }

    protected void onSetUp() {
        // implemented in subclasses
    }

    protected void postSetUp() {
        // implemented in subclasses
    }

    protected void onTearDown() {
        // implemented in subclasses
    }

    protected MockServletContext createMockServletContext() {
        return new MockServletContext();
    }

    protected MockApplicationContext createMockApplicationContext() {
        return new MockApplicationContext();
    }

    protected Resource[] getResources(String pattern) throws IOException {
        return new PathMatchingResourcePatternResolver().getResources(pattern);
    }

    protected MessageSource createMessageSource() {
        return new StaticMessageSource();
    }

}