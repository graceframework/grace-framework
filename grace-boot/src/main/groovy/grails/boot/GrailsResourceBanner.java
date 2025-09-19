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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ResourceBanner;
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
 * Add extra properties related Grails, such as Grails version, application name and version.
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
        List<PropertyResolver> resolvers = new ArrayList<>(super.getPropertyResolvers(environment, sourceClass));
        resolvers.add(getGrailsVersionResolver(environment));
        return resolvers;
    }

    private PropertyResolver getGrailsVersionResolver(Environment environment) {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("grace-name", getNamesMap(environment, "")));
        propertySources.addLast(new MapPropertySource("grace-version", getVersionsMap(environment, "")));
        return new PropertySourcesPropertyResolver(propertySources);
    }

    private Map<String, Object> getNamesMap(Environment environment, String defaultValue) {
        String applicationName = environment.getProperty("info.app.name");
        if (applicationName == null) {
            applicationName = Metadata.getCurrent().getApplicationName();
        }
        if (applicationName == null) {
            applicationName = defaultValue;
        }
        Map<String, Object> names = new HashMap<>();
        names.put("application.name", applicationName);
        return names;
    }

    private Map<String, Object> getVersionsMap(Environment environment, String defaultValue) {
        String appVersion = getApplicationVersion(environment);
        Map<String, Object> versions = new HashMap<>();
        String grailsVersion = GrailsUtil.getGrailsVersion();
        versions.put("application.version", getVersionString(appVersion, false, defaultValue));
        versions.put("grace.version", getVersionString(grailsVersion, false, defaultValue));
        versions.put("application.formatted-version", getVersionString(appVersion, true, defaultValue));
        versions.put("grace.formatted-version", getVersionString(grailsVersion, true, defaultValue));
        return versions;
    }

    private String getApplicationVersion(Environment environment) {
        String appVersion = environment.getProperty("info.app.version");
        if (appVersion == null) {
            appVersion = Metadata.getCurrent().getApplicationVersion();
        }
        return appVersion;
    }

    private String getVersionString(String version, boolean format, String fallback) {
        if (version == null) {
            return fallback;
        }
        return format ? " (v" + version + ")" : version;
    }

}
