/*
 * Copyright 2004-2024 the original author or authors.
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
package grails.util

import java.lang.ref.Reference
import java.lang.ref.SoftReference

import groovy.transform.CompileStatic
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.env.PropertiesPropertySourceLoader
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurablePropertyResolver
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.UrlResource

import grails.io.IOUtils

/**
 * Represents the application Metadata and loading mechanics.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.1
 * @see PropertiesPropertySourceLoader
 * @see YamlPropertySourceLoader
 */
@CompileStatic
class Metadata {

    public static final String FILE = 'application.yml'
    public static final String APPLICATION_VERSION = 'info.app.version'
    public static final String APPLICATION_NAME = 'info.app.name'
    public static final String DEFAULT_APPLICATION_NAME = 'grailsApplication'
    public static final String APPLICATION_GRAILS_VERSION = 'info.app.grailsVersion'
    public static final String SERVLET_VERSION = 'info.app.servletVersion'
    public static final String DEFAULT_SERVLET_VERSION = '6.0'

    private static final Holder<Reference<Metadata>> HOLDER = new Holder<Reference<Metadata>>('Metadata')
    private static final String BUILD_INFO_FILE = 'META-INF/grails.build.info'

    private final MutablePropertySources propertySources = new MutablePropertySources()
    private final ConfigurablePropertyResolver propertyResolver

    private Metadata() {
        loadFromDefault()
        this.propertyResolver = ConfigurationPropertySources.createPropertyResolver(propertySources)
    }

    private Metadata(Map<String, Object> properties) {
        this.propertySources.addFirst(new MapPropertySource("default", properties))
        this.propertyResolver = ConfigurationPropertySources.createPropertyResolver(propertySources)
    }

    private Metadata(InputStream inputStream) {
        loadFromInputStream(inputStream)
        this.propertyResolver = ConfigurationPropertySources.createPropertyResolver(propertySources)
    }

    /**
     * @return the metadata for the current application
     */
    static Metadata getCurrent() {
        Metadata m = getFromMap()
        if (m == null) {
            m = new Metadata()
            HOLDER.set(new SoftReference<Metadata>(m))
        }
        m
    }

    static Metadata getInstance(InputStream inputStream) {
        Metadata m = new Metadata(inputStream)
        HOLDER.set(new FinalReference<Metadata>(m))
        m
    }

    static void reset() {

    }

    private static Metadata getFromMap() {
        Reference<Metadata> metadata = HOLDER.get()
        metadata == null ? null : metadata.get()
    }

    private void loadFromDefault() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
            URL url = classLoader.getResource(FILE)
            if (url == null) {
                url = getClass().getClassLoader().getResource(FILE)
            }
            if (url != null) {
                YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader()
                yamlPropertySourceLoader.load('application', new UrlResource(url)).forEach(this.propertySources::addFirst)
            }

            url = classLoader.getResource(BUILD_INFO_FILE)
            if (url != null) {
                if (IOUtils.isWithinBinary(url) || !Environment.isDevelopmentEnvironmentAvailable()) {
                    PropertiesPropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader()
                    propertiesPropertySourceLoader.load('build.info', new UrlResource(url)).forEach(this.propertySources::addFirst)
                }
            }
            else {
                // try WAR packaging resolve
                url = classLoader.getResource('../../' + BUILD_INFO_FILE)
                if (url != null) {
                    if (IOUtils.isWithinBinary(url) || !Environment.isDevelopmentEnvironmentAvailable()) {
                        PropertiesPropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader()
                        propertiesPropertySourceLoader.load('build.info', new UrlResource(url)).forEach(this.propertySources::addFirst)
                    }
                }
            }
            StandardEnvironment standardEnvironment = new StandardEnvironment()
            this.propertySources.addFirst(
                    new MapPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, standardEnvironment.getSystemProperties()))
            this.propertySources.addFirst(
                    new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, standardEnvironment.getSystemEnvironment()))
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot load application metadata: ${e.getMessage()}", e)
        }
    }

    private void loadYml(InputStream input) {
        new YamlPropertySourceLoader().load('metadata', new InputStreamResource(input)).forEach(this.propertySources::addLast)
    }

    private void loadFromInputStream(InputStream inputStream) {
        loadYml(inputStream)
    }

    /**
     * @return The application version
     */
    String getApplicationVersion() {
        getProperty(APPLICATION_VERSION, String, null)
    }

    /**
     * @return The Grails version used to build the application
     */
    String getGrailsVersion() {
        getProperty(APPLICATION_GRAILS_VERSION, String, getClass().getPackage().getImplementationVersion())
    }

    /**
     * @return The environment the application expects to run in
     */
    String getEnvironment() {
        getProperty(Environment.KEY, String, null)
    }

    /**
     * @return The application name
     */
    String getApplicationName() {
        getProperty(APPLICATION_NAME, String, DEFAULT_APPLICATION_NAME)
    }

    /**
     * @return The version of the servlet spec the application was created for
     */
    String getServletVersion() {
        String servletVersion = getProperty(SERVLET_VERSION, String, System.getProperty(SERVLET_VERSION))
        servletVersion ?: DEFAULT_SERVLET_VERSION
    }

    boolean containsKey(Object key) {
        this.propertyResolver.containsProperty((String) key)
    }

    @Deprecated
    Object get(Object key) {
        getProperty(key.toString(), Object, null)
    }

    @Deprecated
    Object getProperty(String propertyName) {
        get(propertyName)
    }

    <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        this.propertyResolver.getProperty(key, targetType, defaultValue)
    }

    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        T value = getProperty(key, targetType, null)
        if (!value) {
            throw new IllegalStateException("Value for key [$key] cannot be resolved")
        }
    }

    Object navigate(String... path) {
        this.propertyResolver.getProperty(path.join('.').toString(), Object, null)
    }

    static class FinalReference<T> extends SoftReference<T> {

        private final T ref

        FinalReference(T t) {
            super(t)
            ref = t
        }

        @Override
        T get() {
            ref
        }

    }

}
