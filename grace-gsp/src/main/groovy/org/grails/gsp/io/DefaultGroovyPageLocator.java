/*
 * Copyright 2011-2023 the original author or authors.
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
package org.grails.gsp.io;

import java.io.File;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;
import grails.util.BuildSettings;
import grails.util.CollectionUtils;
import grails.util.Environment;

import org.grails.gsp.GroovyPage;
import org.grails.gsp.GroovyPageBinding;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.BinaryGrailsPlugin;
import org.grails.taglib.TemplateVariableBinding;

/**
 * Used to locate GSPs whether in development or WAR deployed mode from static
 * resources, custom resource loaders and binary plugins.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultGroovyPageLocator implements GroovyPageLocator, ResourceLoaderAware, ApplicationContextAware, PluginManagerAware {

    public static final String PATH_TO_WEB_INF_VIEWS = "/WEB-INF/grails-app/views";

    private static final String SLASHED_VIEWS_DIR_PATH = "/" + GrailsResourceUtils.VIEWS_DIR_PATH;

    private static final String PLUGINS_PATH = "/plugins/";

    private static final String GRAILS_VIEWS_PATH = "/" + BuildSettings.GRAILS_APP_PATH + "/" + "views";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Collection<ResourceLoader> resourceLoaders = new ConcurrentLinkedQueue<>();

    protected GrailsPluginManager pluginManager;

    private static final String BLANK = "";

    private ConcurrentMap<String, String> precompiledGspMap;

    protected boolean warDeployed = Environment.isWarDeployed();

    protected boolean reloadEnabled = !this.warDeployed;

    private Set<String> reloadedPrecompiledGspClassNames = new CopyOnWriteArraySet<>();

    public void setResourceLoader(ResourceLoader resourceLoader) {
        addResourceLoader(resourceLoader);
    }

    public void addResourceLoader(ResourceLoader resourceLoader) {
        if (resourceLoader != null && !this.resourceLoaders.contains(resourceLoader)) {
            this.resourceLoaders.add(resourceLoader);
        }
    }

    public void setPrecompiledGspMap(Map<String, String> precompiledGspMap) {
        if (precompiledGspMap == null) {
            this.precompiledGspMap = null;
        }
        else {
            this.precompiledGspMap = new ConcurrentHashMap<>(precompiledGspMap);
        }
    }

    public GroovyPageScriptSource findPage(final String uri) {
        GroovyPageScriptSource scriptSource = findResourceScriptSource(uri);
        if (scriptSource == null) {
            scriptSource = findBinaryScriptSource(uri);
        }
        if (scriptSource == null) {
            scriptSource = findResourceScriptSourceInPlugins(uri);
        }
        return scriptSource;
    }

    protected Resource findReloadablePage(final String uri) {
        Resource resource = findResource(uri);
        if (resource == null) {
            resource = findResourceInPlugins(uri);
        }
        return resource;
    }

    public GroovyPageScriptSource findPageInBinding(String pluginName, String uri, TemplateVariableBinding binding) {

        GroovyPageScriptSource scriptSource = null;
        String contextPath = resolveContextPath(pluginName, uri, binding);
        String fullURI = GrailsResourceUtils.appendPiecesForUri(contextPath, uri);

        if (this.pluginManager != null) {
            GrailsPlugin grailsPlugin = this.pluginManager.getGrailsPlugin(pluginName);
            if (grailsPlugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryGrailsPlugin = (BinaryGrailsPlugin) grailsPlugin;
                File projectDirectory = binaryGrailsPlugin.getProjectDirectory();
                if (projectDirectory != null) {
                    File f = new File(projectDirectory, BuildSettings.GRAILS_APP_PATH + "/views" + uri);
                    if (f.exists()) {
                        scriptSource = new GroovyPageResourceScriptSource(uri, new FileSystemResource(f));
                    }
                }
                else {
                    scriptSource = resolveViewInBinaryPlugin(binaryGrailsPlugin, uri);
                }
            }
        }

        if (scriptSource == null) {
            scriptSource = findPageInBinding(fullURI, binding);
        }

        if (scriptSource == null) {
            scriptSource = findResourceScriptSource(uri);
        }


        //last effort to resolve and force name of plugin to use camel case
        if (scriptSource == null) {
            contextPath = resolveContextPath(pluginName, uri, binding, true);
            scriptSource = findPageInBinding(GrailsResourceUtils.appendPiecesForUri(contextPath, uri), binding);
        }

        return scriptSource;
    }

    protected String resolveContextPath(String pluginName, String uri, TemplateVariableBinding binding) {
        return resolveContextPath(pluginName, uri, binding, false);
    }

    protected String resolveContextPath(String pluginName, String uri, TemplateVariableBinding binding, boolean forceCamelCase) {
        String contextPath = null;

        if (uri.startsWith("/plugins/")) {
            contextPath = BLANK;
        }
        else if (pluginName != null && this.pluginManager != null) {
            contextPath = this.pluginManager.getPluginPath(pluginName);
        }
        else if (binding instanceof GroovyPageBinding) {
            String pluginContextPath = ((GroovyPageBinding) binding).getPluginContextPath();
            contextPath = pluginContextPath != null ? pluginContextPath : BLANK;
        }
        else {
            contextPath = BLANK;
        }

        return contextPath;
    }

    public void removePrecompiledPage(GroovyPageCompiledScriptSource scriptSource) {
        this.reloadedPrecompiledGspClassNames.add(scriptSource.getCompiledClass().getName());
        if (scriptSource.getURI() != null && this.precompiledGspMap != null) {
            this.precompiledGspMap.remove(scriptSource.getURI());
        }
    }

    public GroovyPageScriptSource findPageInBinding(String uri, TemplateVariableBinding binding) {
        GroovyPageScriptSource scriptSource = findResourceScriptSource(uri);

        if (scriptSource == null) {
            GrailsPlugin pagePlugin = binding instanceof GroovyPageBinding ? ((GroovyPageBinding) binding).getPagePlugin() : null;
            if (pagePlugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) pagePlugin;
                scriptSource = resolveViewInBinaryPlugin(binaryPlugin, uri);
            }
            else if (pagePlugin != null) {
                scriptSource = findResourceScriptSource(resolvePluginViewPath(uri, pagePlugin));
            }
        }

        if (scriptSource == null) {
            scriptSource = findBinaryScriptSource(uri);
        }

        return scriptSource;
    }

    protected GroovyPageScriptSource resolveViewInBinaryPlugin(BinaryGrailsPlugin binaryPlugin, String uri) {
        GroovyPageCompiledScriptSource scriptSource = null;
        String fullUri = removeViewLocationPrefixes(uri);
        fullUri = GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, fullUri);
        Class<?> viewClass = binaryPlugin.resolveView(fullUri);
        if (viewClass != null && !this.reloadedPrecompiledGspClassNames.contains(viewClass.getName())) {
            scriptSource = createGroovyPageCompiledScriptSource(uri, fullUri, viewClass);
            // we know we have binary plugin, sp setting to null in the resourceCallable to skip reloading.
            scriptSource.setResourceCallable(null);
        }
        return scriptSource;
    }

    protected GroovyPageCompiledScriptSource createGroovyPageCompiledScriptSource(final String uri, String fullPath, Class<?> viewClass) {
        GroovyPageCompiledScriptSource scriptSource = new GroovyPageCompiledScriptSource(uri, fullPath, viewClass);
        if (this.reloadEnabled) {
            scriptSource.setResourceCallable(new PrivilegedAction<Resource>() {
                public Resource run() {
                    return findReloadablePage(uri);
                }
            });
        }
        return scriptSource;
    }

    protected GroovyPageScriptSource findBinaryScriptSource(String uri) {
        if (this.pluginManager == null) {
            return null;
        }

        List<GrailsPlugin> allPlugins = Arrays.asList(this.pluginManager.getAllPlugins());
        Collections.reverse(allPlugins);

        for (GrailsPlugin plugin : allPlugins) {
            if (!(plugin instanceof BinaryGrailsPlugin)) {
                continue;
            }

            BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
            if (logger.isDebugEnabled()) {
                logger.debug("Searching plugin [{}] for GSP view [{}]", plugin.getName(), uri);
            }
            GroovyPageScriptSource scriptSource = resolveViewInBinaryPlugin(binaryPlugin, uri);
            if (scriptSource != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found GSP view [{}] in plugin [{}]", uri, plugin.getName());
                }
                return scriptSource;
            }
            else if (binaryPlugin.getProjectDirectory() != null) {
                scriptSource = resolveViewInPluginProjectDirectory(binaryPlugin, uri);
                if (scriptSource != null) {
                    return scriptSource;
                }
            }
        }

        return null;
    }

    private GroovyPageScriptSource resolveViewInPluginProjectDirectory(BinaryGrailsPlugin binaryPlugin, String uri) {
        File projectDirectory = binaryPlugin.getProjectDirectory();

        File f = new File(projectDirectory, BuildSettings.GRAILS_APP_PATH + "/views" + uri);
        if (f.exists()) {
            return new GroovyPageResourceScriptSource(uri, new FileSystemResource(f));
        }

        return null;
    }

    protected GroovyPageScriptSource findResourceScriptSourceInPlugins(String uri) {
        if (this.pluginManager == null) {
            return null;
        }

        for (GrailsPlugin plugin : this.pluginManager.getAllPlugins()) {
            if (plugin instanceof BinaryGrailsPlugin) {
                continue;
            }

            GroovyPageScriptSource scriptSource = findResourceScriptSource(resolvePluginViewPath(uri, plugin));
            if (scriptSource != null) {
                return scriptSource;
            }
        }

        return null;
    }

    protected Resource findResourceInPlugins(String uri) {
        if (this.pluginManager == null) {
            return null;
        }

        for (GrailsPlugin plugin : this.pluginManager.getAllPlugins()) {
            if (plugin instanceof BinaryGrailsPlugin) {
                continue;
            }

            String pluginViewPath = resolvePluginViewPath(uri, plugin);
            Resource resource = findResource(pluginViewPath);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    protected String resolvePluginViewPath(String uri, GrailsPlugin plugin) {
        uri = removeViewLocationPrefixes(uri);
        return GrailsResourceUtils.appendPiecesForUri(plugin.getPluginPath(), GrailsResourceUtils.VIEWS_DIR_PATH, uri);
    }

    protected String removeViewLocationPrefixes(String uri) {
        uri = removePrefix(uri, GrailsResourceUtils.WEB_INF);
        uri = removePrefix(uri, SLASHED_VIEWS_DIR_PATH);
        uri = removePrefix(uri, GrailsResourceUtils.VIEWS_DIR_PATH);
        return uri;
    }

    protected String removePrefix(String uri, String prefix) {
        if (uri.startsWith(prefix)) {
            uri = uri.substring(prefix.length());
        }
        return uri;
    }

    protected GroovyPageScriptSource findResourceScriptSource(final String uri) {
        List<String> searchPaths = resolveSearchPaths(uri);

        return findResourceScriptPathForSearchPaths(uri, searchPaths);
    }

    protected List<String> resolveSearchPaths(String uri) {
        List<String> searchPaths = null;

        uri = removeViewLocationPrefixes(uri);
        if (this.warDeployed) {
            if (uri.startsWith(PLUGINS_PATH)) {
                PluginViewPathInfo pathInfo = getPluginViewPathInfo(uri);

                searchPaths = CollectionUtils.newList(
                        GrailsResourceUtils.appendPiecesForUri(GrailsResourceUtils.WEB_INF, PLUGINS_PATH,
                                pathInfo.pluginName, GrailsResourceUtils.VIEWS_DIR_PATH, pathInfo.path),
                        GrailsResourceUtils.appendPiecesForUri(GrailsResourceUtils.WEB_INF, uri),
                        uri);
            }
            else {
                searchPaths = CollectionUtils.newList(
                        GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri),
                        uri);
            }
        }
        else {
            searchPaths = CollectionUtils.newList(
                    GrailsResourceUtils.appendPiecesForUri(GRAILS_VIEWS_PATH, uri),
                    GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri),
                    uri);
        }
        return searchPaths;
    }

    @SuppressWarnings("unchecked")
    protected GroovyPageScriptSource findResourceScriptPathForSearchPaths(String uri, List<String> searchPaths) {
        if (isPrecompiledAvailable()) {
            for (String searchPath : searchPaths) {
                String gspClassName = this.precompiledGspMap.get(searchPath);
                if (gspClassName != null && !this.reloadedPrecompiledGspClassNames.contains(gspClassName)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found pre-compiled GSP template [{}] for path [{}]", gspClassName, searchPath);
                    }
                    Class<GroovyPage> gspClass = null;
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Loading GSP template [{}]", gspClassName);
                        }
                        gspClass = (Class<GroovyPage>) Class.forName(gspClassName, true, Thread.currentThread().getContextClassLoader());
                    }
                    catch (ClassNotFoundException e) {
                        logger.warn("Cannot load class " + gspClassName + ". Resuming on non-precompiled implementation.", e);
                    }
                    if (gspClass != null) {
                        GroovyPageCompiledScriptSource groovyPageCompiledScriptSource =
                                createGroovyPageCompiledScriptSource(uri, searchPath, gspClass);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Returning new GSP script source for class [{}]", gspClassName);
                        }
                        return groovyPageCompiledScriptSource;
                    }
                }
            }
        }

        Resource foundResource = findResource(searchPaths);
        return foundResource == null ? null : new GroovyPageResourceScriptSource(uri, foundResource);
    }

    protected Resource findResource(String uri) {
        return findResource(resolveSearchPaths(uri));
    }

    protected Resource findResource(List<String> searchPaths) {
        Resource foundResource = null;
        Resource resource;
        for (ResourceLoader loader : this.resourceLoaders) {
            for (String path : searchPaths) {
                resource = loader.getResource(path);
                if (resource != null && resource.exists()) {
                    foundResource = resource;
                    break;
                }
            }
            if (foundResource != null) {
                break;
            }
        }
        return foundResource;
    }

    private boolean isPrecompiledAvailable() {
        return this.precompiledGspMap != null && this.precompiledGspMap.size() > 0;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        addResourceLoader(applicationContext);
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public static PluginViewPathInfo getPluginViewPathInfo(String uri) {
        return new PluginViewPathInfo(uri);
    }

    public boolean isReloadEnabled() {
        return this.reloadEnabled;
    }

    public void setReloadEnabled(boolean reloadEnabled) {
        this.reloadEnabled = reloadEnabled;
    }

    public static class PluginViewPathInfo {

        public String basePath;

        public String pluginName;

        public String path;

        public PluginViewPathInfo(String uri) {
            this.basePath = uri.substring(PLUGINS_PATH.length(), uri.length());
            int i = this.basePath.indexOf("/");
            if (i > -1) {
                this.pluginName = this.basePath.substring(0, i);
                this.path = this.basePath.substring(i, this.basePath.length());
            }
        }

    }

}
