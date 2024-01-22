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
package grails.util

import java.lang.ref.Reference
import java.lang.ref.SoftReference

import groovy.transform.CompileStatic
import io.micronaut.context.env.PropertiesPropertySourceLoader
import io.micronaut.context.env.PropertySource
import io.micronaut.context.env.PropertySourcePropertyResolver
import io.micronaut.context.env.SystemPropertiesPropertySource
import io.micronaut.context.env.yaml.YamlPropertySourceLoader
import io.micronaut.core.value.PropertyResolver

import grails.io.IOUtils

import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource
import org.grails.io.support.UrlResource

/**
 * Represents the application Metadata and loading mechanics.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@CompileStatic
class Metadata extends PropertySourcePropertyResolver {

    private static final long serialVersionUID = -582452926111226898L
    public static final String FILE = 'application.yml'
    public static final String APPLICATION_VERSION = 'info.app.version'
    public static final String APPLICATION_NAME = 'info.app.name'
    public static final String DEFAULT_APPLICATION_NAME = 'grailsApplication'
    public static final String APPLICATION_GRAILS_VERSION = 'info.app.grailsVersion'
    public static final String SERVLET_VERSION = 'info.app.servletVersion'
    public static final String WAR_DEPLOYED = 'info.app.warDeployed'
    public static final String DEFAULT_SERVLET_VERSION = '3.0'

    private static final Holder<Reference<Metadata>> HOLDER = new Holder<Reference<Metadata>>('Metadata')
    private static final String BUILD_INFO_FILE = 'META-INF/grails.build.info'

    private Resource metadataFile
    private boolean warDeployed
    private String servletVersion = DEFAULT_SERVLET_VERSION
    private Map<String, Object> props = null

    private Metadata() {
        loadFromDefault()
    }

    private Metadata(Resource res) {
        metadataFile = res
        loadFromFile(res)
    }

    private Metadata(File f) {
        metadataFile = new FileSystemResource(f)
        loadFromFile(metadataFile)
    }

    private Metadata(InputStream inputStream) {
        loadFromInputStream(inputStream)
    }

    private Metadata(Map<String, String> properties) {
        props = new LinkedHashMap<String, Object>(properties)
        addPropertySource(PropertySource.of(props))
        afterLoading()
    }

    Resource getMetadataFile() {
        metadataFile
    }

    /**
     * Resets the current state of the Metadata so it is re-read.
     */
    static void reset() {
        Metadata m = getFromMap()
        if (m != null) {
            m.clear()
            m.afterLoading()
        }
    }

    private void afterLoading() {
        // allow override via system properties
        PropertySource systemPropertiesPropertySource = new SystemPropertiesPropertySource()
        addPropertySource(systemPropertiesPropertySource)

        if (!containsProperty(APPLICATION_NAME)) {
            Map<String, Object> m = [(APPLICATION_NAME): (Object) DEFAULT_APPLICATION_NAME]
            addPropertySource('appName', m)
            resetCaches()
        }
        warDeployed = ((PropertyResolver) this).getProperty(WAR_DEPLOYED, Boolean).orElse(false)
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

    private void loadFromDefault() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
            URL url = classLoader.getResource(FILE)
            if (url == null) {
                url = getClass().getClassLoader().getResource(FILE)
            }
            if (url != null) {
                url.withInputStream { InputStream input ->
                    addPropertySource(PropertySource.of('application', new YamlPropertySourceLoader().read('application', input)))
                }
                this.metadataFile = new UrlResource(url)
            }

            url = classLoader.getResource(BUILD_INFO_FILE)
            if (url != null) {
                if (IOUtils.isWithinBinary(url) || !Environment.isDevelopmentEnvironmentAvailable()) {
                    url.withInputStream { InputStream input ->
                        Map<String, Object> buildInfo = new PropertiesPropertySourceLoader().read('build.info', input)
                        addPropertySource(PropertySource.of('build.info', buildInfo))
                    }
                }
            }
            else {
                // try WAR packaging resolve
                url = classLoader.getResource('../../' + BUILD_INFO_FILE)
                if (url != null) {
                    if (IOUtils.isWithinBinary(url) || !Environment.isDevelopmentEnvironmentAvailable()) {
                        url.withInputStream { InputStream input ->
                            Map<String, Object> buildInfo = new PropertiesPropertySourceLoader().read('build.info', input)
                            addPropertySource(PropertySource.of('build.info', buildInfo))
                        }
                    }
                }
            }
            afterLoading()
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot load application metadata: ${e.getMessage()}", e)
        }
    }

    private void loadYml(InputStream input) {
        addPropertySource(PropertySource.of(new YamlPropertySourceLoader().read('metadata', input)))
    }

    private void loadFromInputStream(InputStream inputStream) {
        loadYml(inputStream)
        afterLoading()
    }

    private void loadFromFile(Resource file) {
        if (file?.exists()) {
            InputStream input = null
            try {
                input = file.getInputStream()
                loadYml(input)
                afterLoading()
            }
            catch (Exception e) {
                throw new RuntimeException('Cannot load application metadata:' + e.getMessage(), e)
            }
            finally {
                closeQuietly(input)
            }
        }
    }

    /**
     * Loads a Metadata instance from a Reader
     * @param inputStream The InputStream
     * @return a Metadata instance
     */
    static Metadata getInstance(InputStream inputStream) {
        Metadata m = new Metadata(inputStream)
        HOLDER.set(new FinalReference<Metadata>(m))
        m
    }

    /**
     * Loads and returns a new Metadata object for the given File.
     * @param file The File
     * @return A Metadata object
     */
    static Metadata getInstance(File file) {
        getInstance(new FileSystemResource(file))
    }

    /**
     * Loads and returns a new Metadata object for the given File.
     * @param file The File
     * @return A Metadata object
     */
    static Metadata getInstance(Resource file) {
        Reference<Metadata> ref = HOLDER.get()
        if (ref != null) {
            Metadata metadata = ref.get()
            if (metadata != null && metadata.getMetadataFile() != null && metadata.getMetadataFile() == file) {
                return metadata
            }
            createAndBindNew(file)
        }
        createAndBindNew(file)
    }

    private static Metadata createAndBindNew(Resource file) {
        Metadata m = new Metadata(file)
        HOLDER.set(new FinalReference<Metadata>(m))
        m
    }

    /**
     * Reloads the application metadata.
     * @return The metadata object
     */
    static Metadata reload() {
        Resource f = getCurrent().getMetadataFile()
        if (f?.exists()) {
            return getInstance(f)
        }

        f == null ? new Metadata() : new Metadata(f)
    }

    /**
     * @return The application version
     */
    String getApplicationVersion() {
        ((PropertyResolver) this).getProperty(APPLICATION_VERSION, String).orElse(null)
    }

    /**
     * @return The Grails version used to build the application
     */
    String getGrailsVersion() {
        ((PropertyResolver) this).getProperty(APPLICATION_GRAILS_VERSION, String)
                .orElse(getClass().getPackage().getImplementationVersion())
    }

    /**
     * @return The environment the application expects to run in
     */
    String getEnvironment() {
        ((PropertyResolver) this).getProperty(Environment.KEY, String).orElse(null)
    }

    /**
     * @return The application name
     */
    String getApplicationName() {
        ((PropertyResolver) this).getProperty(APPLICATION_NAME, String).orElse(DEFAULT_APPLICATION_NAME)
    }

    /**
     * @return The version of the servlet spec the application was created for
     */
    String getServletVersion() {
        Optional<String> servletVersion = ((PropertyResolver) this).getProperty(SERVLET_VERSION, String)
        if (!servletVersion.isPresent()) {
            servletVersion = Optional.ofNullable(System.getProperty(SERVLET_VERSION))
        }
        servletVersion.orElse(DEFAULT_SERVLET_VERSION)
    }

    void setServletVersion(String servletVersion) {
        this.servletVersion = servletVersion
    }

    /**
     * @return true if this application is deployed as a WAR
     */
    boolean isWarDeployed() {
        Environment.isWarDeployed()
    }

    /**
     * @return True if the development sources are present
     */
    boolean isDevelopmentEnvironmentAvailable() {
        Environment.isDevelopmentEnvironmentAvailable()
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close()
            }
            catch (Exception ignored) {
            }
        }
    }

    private static Metadata getFromMap() {
        Reference<Metadata> metadata = HOLDER.get()
        metadata == null ? null : metadata.get()
    }

    boolean containsKey(Object key) {
        containsProperty(key.toString())
    }

    @Deprecated
    Object get(Object key) {
        ((PropertyResolver) this).getProperty(key.toString(), Object).orElse(null)
    }

    void clear() {
        propertySources.clear()
        clearCatalog(rawCatalog)
        clearCatalog(nonGenerated)
        clearCatalog(catalog)
        resetCaches()
        if (metadataFile != null) {
            loadFromFile(metadataFile)
        }
        else if (props != null) {
            addPropertySource(PropertySource.of(props))
            afterLoading()
        }
        else {
            loadFromDefault()
        }
    }

    private void clearCatalog(Map<String, Object>[] catalog) {
        synchronized (catalog) {
            for (int i = 0; i < catalog.length; i++) {
                catalog[i] = null
            }
        }
    }

    Object getOrDefault(Object key, Object defaultValue) {
        ((PropertyResolver) this).getProperty(key.toString(), Object).orElse(defaultValue)
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

    @Override
    <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        ((PropertyResolver) this).getProperty(key, targetType).orElse(defaultValue)
    }

    @Override
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        ((PropertyResolver) this).getProperty(key, Object)
                .map(value -> value.asType(targetType))
                .orElseThrow(() -> new IllegalStateException("Value for key [$key] cannot be resolved"))
    }

    @Deprecated
    Object navigate(String... path) {
        ((Optional<Object>) ((PropertyResolver) this).getProperty(path.join('.').toString(), Object)).orElse(null)
    }

    @Deprecated
    Object getProperty(String propertyName) {
        get(propertyName)
    }

}
