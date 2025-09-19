/*
 * Copyright 2021-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ResourceBanner;
import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;

import grails.util.GrailsUtil;
import grails.util.Metadata;

/**
 * Banner implementation that prints from a source text {@link Resource}.
 * Add extra properties related Grails, such as Grails version, application name, title and version.
 *
 * @author Michael Yan
 * @since 2022.0.0
 * @see ResourceBanner
 */
public class GrailsResourceBanner extends ResourceBanner {

    public GrailsResourceBanner(Resource resource) {
        super(resource);
    }

    @Override
    protected List<PropertyResolver> getPropertyResolvers(Environment environment, Class<?> sourceClass) {
        List<PropertyResolver> resolvers = new ArrayList<>();
        resolvers.add(new PropertySourcesPropertyResolver(createNullDefaultSources(environment, sourceClass)));
        resolvers.add(new PropertySourcesPropertyResolver(createEmptyDefaultSources(environment, sourceClass)));
        return resolvers;
    }

    private MutablePropertySources createNullDefaultSources(Environment environment, Class<?> sourceClass) {
        MutablePropertySources nullDefaultSources = new MutablePropertySources();
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            configurableEnvironment.getPropertySources().forEach(nullDefaultSources::addLast);
        }
        nullDefaultSources.addLast(getNameSource(sourceClass, environment, null));
        nullDefaultSources.addLast(getTitleSource(sourceClass, environment, null));
        nullDefaultSources.addLast(getAnsiSource());
        nullDefaultSources.addLast(getVersionSource(sourceClass, environment, null));
        return nullDefaultSources;
    }

    private MutablePropertySources createEmptyDefaultSources(Environment environment, Class<?> sourceClass) {
        MutablePropertySources emptyDefaultSources = new MutablePropertySources();
        emptyDefaultSources.addLast(getNameSource(sourceClass, environment, ""));
        emptyDefaultSources.addLast(getTitleSource(sourceClass, environment, ""));
        emptyDefaultSources.addLast(getVersionSource(sourceClass, environment, ""));
        return emptyDefaultSources;
    }

    private MapPropertySource getNameSource(Class<?> sourceClass, Environment environment, String defaultValue) {
        String applicationName = getApplicationName(sourceClass, environment);
        Map<String, Object> nameMap = Collections.singletonMap("application.name",
                (applicationName != null) ? applicationName : defaultValue);
        return new MapPropertySource("name", nameMap);
    }

    protected String getApplicationName(Class<?> sourceClass, Environment environment) {
        String applicationName = environment.getProperty("info.app.name");
        if (applicationName == null) {
            applicationName = environment.getProperty("spring.application.name");
        }
        if (applicationName == null) {
            applicationName = Metadata.getCurrent().getApplicationName();
        }
        return applicationName;
    }

    private MapPropertySource getTitleSource(Class<?> sourceClass, Environment environment, String defaultValue) {
        String applicationTitle = getApplicationTitle(sourceClass);
        if (applicationTitle == null) {
            applicationTitle = getApplicationTitle(environment);
        }
        Map<String, Object> titleMap = Collections.singletonMap("application.title",
                (applicationTitle != null) ? applicationTitle : defaultValue);
        return new MapPropertySource("title", titleMap);
    }

    protected String getApplicationTitle(Environment environment) {
        return environment.getProperty("info.app.title");
    }

    private AnsiPropertySource getAnsiSource() {
        return new AnsiPropertySource("ansi", true);
    }

    private MapPropertySource getVersionSource(Class<?> sourceClass, Environment environment, String defaultValue) {
        return new MapPropertySource("version", getVersionsMap(sourceClass, environment, defaultValue));
    }

    private Map<String, Object> getVersionsMap(Class<?> sourceClass, Environment environment, String defaultValue) {
        String appVersion = getApplicationVersion(environment);
        Map<String, Object> versions = new HashMap<>();
        String bootVersion = getBootVersion();
        String grailsVersion = getGrailsVersion();
        versions.put("application.version", getVersionString(appVersion, false, defaultValue));
        versions.put("grace.version", getVersionString(grailsVersion, false, defaultValue));
        versions.put("spring-boot.version", getVersionString(bootVersion, false, defaultValue));
        versions.put("application.formatted-version", getVersionString(appVersion, true, defaultValue));
        versions.put("grace.formatted-version", getVersionString(grailsVersion, true, defaultValue));
        versions.put("spring-boot.formatted-version", getVersionString(bootVersion, true, defaultValue));
        return versions;
    }

    @SuppressWarnings("removal")
    @Deprecated(since = "2024.0.0", forRemoval = true)
    protected String getApplicationVersion(Class<?> sourceClass) {
        String applicationVersion = Metadata.getCurrent().getApplicationVersion();
        return (applicationVersion != null) ? applicationVersion : super.getApplicationVersion(sourceClass);
    }

    private String getApplicationVersion(Environment environment) {
        String appVersion = environment.getProperty("info.app.version");
        if (appVersion == null) {
            appVersion = environment.getProperty("spring.application.version");
        }
        if (appVersion == null) {
            appVersion = Metadata.getCurrent().getApplicationVersion();
        }
        return appVersion;
    }

    protected String getGrailsVersion() {
        return GrailsUtil.getGrailsVersion();
    }

    private String getVersionString(String version, boolean format, String fallback) {
        if (version == null) {
            return fallback;
        }
        return format ? " (v" + version + ")" : version;
    }

}
