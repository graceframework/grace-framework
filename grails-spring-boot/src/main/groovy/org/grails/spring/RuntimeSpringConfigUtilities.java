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
package org.grails.spring;

import java.io.IOException;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

import grails.spring.BeanBuilder;

/**
 * @since 2.4
 * @author Graeme Rocher
 */
public final class RuntimeSpringConfigUtilities {

    private static final Log logger = LogFactory.getLog(RuntimeSpringConfigUtilities.class);

    public static final String GRAILS_URL_MAPPINGS = "grailsUrlMappings";

    public static final String SPRING_RESOURCES_XML = "classpath:spring/resources.xml";

    public static final String SPRING_RESOURCES_GROOVY = "classpath:spring/resources.groovy";

    public static final String SPRING_RESOURCES_CLASS = "resources";

    private static final String DEVELOPMENT_SPRING_RESOURCES_XML = "file:./grails-app/conf/spring/resources.xml";

    private static volatile BeanBuilder springGroovyResourcesBeanBuilder = null;

    private RuntimeSpringConfigUtilities() {
    }

    /**
     * Attempt to load the beans defined by a BeanBuilder DSL closure in "resources.groovy".
     *
     * @param config
     * @param context
     */
    private static void doLoadSpringGroovyResources(RuntimeSpringConfiguration config,
            Map<String, Object> variables, GenericApplicationContext context) {

        loadExternalSpringConfig(config, variables);
        if (context != null) {
            springGroovyResourcesBeanBuilder.registerBeans(context);
        }
    }

    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object.
     * @param config The config instance
     */
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, Map<String, Object> variables) {
        if (springGroovyResourcesBeanBuilder == null) {
            try {
                Class<?> groovySpringResourcesClass = null;
                try {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    groovySpringResourcesClass = ClassUtils.forName(SPRING_RESOURCES_CLASS, classLoader);
                }
                catch (ClassNotFoundException ignored) {
                }
                if (groovySpringResourcesClass != null) {
                    reloadSpringResourcesConfig(config, variables, groovySpringResourcesClass);
                }
            }
            catch (Exception ex) {
                logger.error("[RuntimeConfiguration] Unable to load beans from resources.groovy", ex);
            }
        }
        else {
            if (!springGroovyResourcesBeanBuilder.getSpringConfig().equals(config)) {
                springGroovyResourcesBeanBuilder.registerBeans(config);
            }
        }
    }

    public static BeanBuilder reloadSpringResourcesConfig(RuntimeSpringConfiguration config,
            Map<String, Object> variables, Class<?> groovySpringResourcesClass)
            throws InstantiationException, IllegalAccessException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        springGroovyResourcesBeanBuilder = new BeanBuilder(null, config, classLoader);
        springGroovyResourcesBeanBuilder.setBinding(new Binding(variables));
        springGroovyResourcesBeanBuilder.setBeanBuildResource(new DescriptiveResource(groovySpringResourcesClass.getName()));
        Script script = (Script) groovySpringResourcesClass.newInstance();
        script.run();
        Object beans = script.getProperty("beans");
        springGroovyResourcesBeanBuilder.beans((Closure<?>) beans);
        return springGroovyResourcesBeanBuilder;
    }

    public static BeanBuilder reloadSpringResourcesConfig(RuntimeSpringConfiguration config,
            Map<String, Object> variables, Resource resource)
            throws InstantiationException, IllegalAccessException, IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        GroovyClassLoader gcl = new GroovyClassLoader(classLoader);
        Class<?> groovySpringResourcesClass = gcl.parseClass(new GroovyCodeSource(resource.getURI()));
        springGroovyResourcesBeanBuilder = new BeanBuilder(null, config, classLoader);
        springGroovyResourcesBeanBuilder.setBinding(new Binding(variables));
        springGroovyResourcesBeanBuilder.setBeanBuildResource(resource);
        Script script = (Script) groovySpringResourcesClass.newInstance();
        script.run();
        Object beans = script.getProperty("beans");
        springGroovyResourcesBeanBuilder.beans((Closure<?>) beans);
        return springGroovyResourcesBeanBuilder;
    }

    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, Map<String, Object> variables) {
        loadExternalSpringConfig(config, variables);
    }

    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, Map<String, Object> variables,
            GenericApplicationContext context) {

        loadExternalSpringConfig(config, variables);
        doLoadSpringGroovyResources(config, variables, context);
    }

    /**
     * Resets the GrailsRuntimeConfigurator.
     */
    public static void reset() {
        springGroovyResourcesBeanBuilder = null;
    }

}
