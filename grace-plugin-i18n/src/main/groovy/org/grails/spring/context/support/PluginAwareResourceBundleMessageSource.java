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
package org.grails.spring.context.support;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import grails.core.GrailsApplicationClass;
import grails.core.support.GrailsApplicationAware;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;
import grails.util.BuildSettings;
import grails.util.CacheEntry;
import grails.util.Environment;
import grails.util.GrailsStringUtils;

import org.grails.core.io.CachingPathMatchingResourcePatternResolver;
import org.grails.core.support.internal.tools.ClassRelativeResourcePatternResolver;
import org.grails.plugins.BinaryGrailsPlugin;

/**
 * A ReloadableResourceBundleMessageSource that is capable of loading message sources from plugins.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.1
 */
public class PluginAwareResourceBundleMessageSource extends ReloadableResourceBundleMessageSource
        implements GrailsApplicationAware, PluginManagerAware, InitializingBean, SmartInitializingSingleton {

    private static final String GRAILS_APP_I18N_PATH_COMPONENT = "/grails-app/i18n/";

    protected GrailsApplication application;

    protected GrailsPluginManager pluginManager;

    private PathMatchingResourcePatternResolver resourceResolver;

    private final ConcurrentMap<Locale, CacheEntry<PropertiesHolder>> cachedMergedPluginProperties = new ConcurrentHashMap<>();

    private final ConcurrentMap<Locale, CacheEntry<PropertiesHolder>> cachedMergedBinaryPluginProperties = new ConcurrentHashMap<>();

    private long pluginCacheMillis = Long.MIN_VALUE;

    private boolean searchClasspath = false;

    private String messageBundleLocationPattern = "classpath*:*.properties";

    public PluginAwareResourceBundleMessageSource() {
    }

    public PluginAwareResourceBundleMessageSource(GrailsApplication application, GrailsPluginManager pluginManager) {
        this.application = application;
        this.pluginManager = pluginManager;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.application = grailsApplication;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void setResourceResolver(PathMatchingResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.pluginManager, "GrailsPluginManager is required");
        Assert.notNull(this.resourceResolver, "PathMatchingResourcePatternResolver is required");

        if (this.pluginCacheMillis == Long.MIN_VALUE) {
            this.pluginCacheMillis = cacheMillis;
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        Resource[] resources;
        if (Environment.isDevelopmentEnvironmentAvailable()) {
            File[] propertiesFiles = null;
            if (new File(BuildSettings.GRAILS_APP_DIR, "i18n").exists()) {
                propertiesFiles = new File(BuildSettings.GRAILS_APP_DIR, "i18n")
                        .listFiles((dir, name) -> name.endsWith(".properties"));
            }

            if (propertiesFiles != null && propertiesFiles.length > 0) {
                List<Resource> resourceList = new ArrayList<>(propertiesFiles.length);
                for (File propertiesFile : propertiesFiles) {
                    resourceList.add(new FileSystemResource(propertiesFile));
                }
                resources = resourceList.toArray(new Resource[0]);
            }
            else {
                resources = new Resource[0];
            }
        }
        else {
            try {
                if (this.searchClasspath) {
                    resources = this.resourceResolver.getResources(this.messageBundleLocationPattern);
                }
                else {
                    DefaultGrailsApplication defaultGrailsApplication = (DefaultGrailsApplication) this.application;
                    if (defaultGrailsApplication != null) {
                        GrailsApplicationClass applicationClass = defaultGrailsApplication.getApplicationClass();
                        if (applicationClass != null) {
                            ResourcePatternResolver resourcePatternResolver = new ClassRelativeResourcePatternResolver(applicationClass.getClass());
                            resources = resourcePatternResolver.getResources(this.messageBundleLocationPattern);
                        }
                        else {
                            resources = this.resourceResolver.getResources(this.messageBundleLocationPattern);
                        }
                    }
                    else {
                        resources = this.resourceResolver.getResources(this.messageBundleLocationPattern);
                    }
                }
            }
            catch (Exception e) {
                resources = new Resource[0];
            }
        }

        List<String> basenames = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            String baseName = GrailsStringUtils.getFileBasename(filename);
            int i = baseName.indexOf('_');
            if (i > -1) {
                baseName = baseName.substring(0, i);
            }
            if (!basenames.contains(baseName) && !baseName.equals("")) {
                basenames.add(baseName);
            }
        }

        setBasenames(basenames.toArray(new String[0]));
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        String msg = super.resolveCodeWithoutArguments(code, locale);
        return msg == null ? resolveCodeWithoutArgumentsFromPlugins(code, locale) : msg;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        MessageFormat mf = super.resolveCode(code, locale);
        return mf == null ? resolveCodeFromPlugins(code, locale) : mf;
    }

    /**
     * Get a PropertiesHolder that contains the actually visible properties
     * for a Locale, after merging all specified resource bundles.
     * Either fetches the holder from the cache or freshly loads it.
     * <p>Only used when caching resource bundle contents forever, i.e.
     * with cacheSeconds &lt; 0. Therefore, merged properties are always
     * cached forever.
     */
    protected PropertiesHolder getMergedPluginProperties(final Locale locale) {
        return CacheEntry.getValue(this.cachedMergedPluginProperties, locale, cacheMillis, () -> {
            Properties mergedProps = new Properties();
            PropertiesHolder mergedHolder = new PropertiesHolder(mergedProps);
            mergeBinaryPluginProperties(locale, mergedProps);
            return mergedHolder;
        });
    }

    /**
     * Attempts to resolve a String for the code from the list of plugin base names
     *
     * @param code The code
     * @param locale The locale
     * @return a MessageFormat
     */
    protected String resolveCodeWithoutArgumentsFromPlugins(String code, Locale locale) {
        if (this.pluginCacheMillis < 0) {
            PropertiesHolder propHolder = getMergedPluginProperties(locale);
            return propHolder.getProperty(code);
        }
        else {
            return findCodeInBinaryPlugins(code, locale);
        }
    }

    protected PropertiesHolder getMergedBinaryPluginProperties(final Locale locale) {
        return CacheEntry.getValue(this.cachedMergedBinaryPluginProperties, locale, cacheMillis, () -> {
            Properties mergedProps = new Properties();
            PropertiesHolder mergedHolder = new PropertiesHolder(mergedProps);
            mergeBinaryPluginProperties(locale, mergedProps);
            return mergedHolder;
        });
    }

    protected void mergeBinaryPluginProperties(final Locale locale, Properties mergedProps) {
        GrailsPlugin[] allPlugins = this.pluginManager.getAllPlugins();
        for (GrailsPlugin plugin : allPlugins) {
            if (plugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
                Properties binaryPluginProperties = binaryPlugin.getProperties(locale);
                if (binaryPluginProperties != null) {
                    mergedProps.putAll(binaryPluginProperties);
                }
            }
        }
    }

    private String findCodeInBinaryPlugins(String code, Locale locale) {
        return getMergedBinaryPluginProperties(locale).getProperty(code);
    }

    private MessageFormat findMessageFormatInBinaryPlugins(String code, Locale locale) {
        return getMergedBinaryPluginProperties(locale).getMessageFormat(code, locale);
    }

    /**
     * Attempts to resolve a MessageFormat for the code from the list of plugin base names
     *
     * @param code The code
     * @param locale The locale
     * @return a MessageFormat
     */
    protected MessageFormat resolveCodeFromPlugins(String code, Locale locale) {
        if (this.pluginCacheMillis < 0) {
            PropertiesHolder propHolder = getMergedPluginProperties(locale);
            return propHolder.getMessageFormat(code, locale);
        }
        else {
            return findMessageFormatInBinaryPlugins(code, locale);
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        super.setResourceLoader(resourceLoader);

        if (this.resourceResolver == null) {
            this.resourceResolver = new CachingPathMatchingResourcePatternResolver(resourceLoader);
        }
    }


    /**
     * Set the number of seconds to cache the list of matching properties files loaded from plugin.
     * <ul>
     * <li>Default value is the same value as cacheSeconds
     * </ul>
     */
    public void setPluginCacheSeconds(int pluginCacheSeconds) {
        this.pluginCacheMillis = pluginCacheSeconds * 1000L;
    }

    /**
     * Whether to search the full classpath for message bundles. Enabling this will degrade startup performance.
     * The default is to only search for message bundles relative to the application classes directory.
     *
     * @param searchClasspath True if the entire classpath should be searched
     */
    public void setSearchClasspath(boolean searchClasspath) {
        this.searchClasspath = searchClasspath;
    }

    /**
     * The location pattern for message bundles
     *
     * @param messageBundleLocationPattern The message bundle location pattern
     */
    public void setMessageBundleLocationPattern(String messageBundleLocationPattern) {
        this.messageBundleLocationPattern = messageBundleLocationPattern;
    }

}
