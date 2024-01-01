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
package grails.plugins;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.Assert;

import grails.core.GrailsApplication;
import grails.core.support.ParentApplicationContextAware;
import grails.plugins.exceptions.PluginException;
import grails.util.Environment;
import grails.util.GrailsClassUtils;

import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.core.io.CachingPathMatchingResourcePatternResolver;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.AbstractGrailsPluginManager;
import org.grails.plugins.BinaryGrailsPlugin;
import org.grails.plugins.BinaryGrailsPluginDescriptor;
import org.grails.plugins.CorePluginFinder;
import org.grails.plugins.DefaultDynamicGrailsPlugin;
import org.grails.plugins.DefaultGrailsPlugin;
import org.grails.plugins.DynamicBinaryGrailsPlugin;
import org.grails.plugins.IdentityPluginFilter;
import org.grails.plugins.PluginFilterRetriever;
import org.grails.spring.DefaultRuntimeSpringConfiguration;
import org.grails.spring.RuntimeSpringConfiguration;

/**
 * <p>Handles the loading and management of plugins in the Grails system.
 * A plugin is just like a normal Grails application except that it contains a file ending
 * in *Plugin.groovy in the root of the directory.
 * <p/>
 * <p>A Plugin class is a Groovy class that has a version and optionally closures
 * called doWithSpring, doWithContext and doWithWebDescriptor
 * <p/>
 * <p>The doWithSpring closure uses the BeanBuilder syntax (@see grails.spring.BeanBuilder) to
 * provide runtime configuration of Grails via Spring
 * <p/>
 * <p>The doWithContext closure is called after the Spring ApplicationContext is built and accepts
 * a single argument (the ApplicationContext)
 * <p/>
 * <p>The doWithWebDescriptor uses mark-up building to provide additional functionality to the web.xml
 * file
 * <p/>
 * <p> Example:
 * <pre>
 * class ClassEditorGrailsPlugin {
 *      def version = '1.1'
 *      def doWithSpring = { application -gt;
 *          classEditor(org.springframework.beans.propertyeditors.ClassEditor, application.classLoader)
 *      }
 * }
 * </pre>
 * <p/>
 * <p>A plugin can also define "dependsOn" and "evict" properties that specify what plugins the plugin
 * depends on and which ones it is incompatible with and should evict
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class DefaultGrailsPluginManager extends AbstractGrailsPluginManager {

    private static final Log logger = LogFactory.getLog(DefaultGrailsPluginManager.class);

    protected static final Class<?>[] COMMON_CLASSES = {
            Boolean.class, Byte.class, Character.class, Class.class, Double.class, Float.class,
            Integer.class, Long.class, Number.class, Short.class, String.class, BigInteger.class,
            BigDecimal.class, URL.class, URI.class };

    private static final String GRAILS_VERSION = "grailsVersion";

    private static final String GRAILS_PLUGIN_SUFFIX = "GrailsPlugin";

    private final List<GrailsPlugin> delayedLoadPlugins = new LinkedList<>();

    private ApplicationContext parentCtx;

    private PathMatchingResourcePatternResolver resolver;

    private final Map<GrailsPlugin, String[]> delayedEvictions = new HashMap<>();

    private final Map<String, Set<GrailsPlugin>> pluginToObserverMap = new HashMap<>();

    private PluginFilter pluginFilter;

    private List<GrailsPlugin> allPluginsOrdered = new ArrayList<>();

    private List<GrailsPlugin> allPlugins = new ArrayList<>();

    private List<GrailsPlugin> userPlugins = new ArrayList<>();

    private List<GrailsPlugin> corePlugins = new ArrayList<>();

    private List<GrailsPlugin> unenabledPlugins = new ArrayList<>();

    public DefaultGrailsPluginManager(String resourcePath, GrailsApplication application) {
        super(application);
        Assert.notNull(application, "Argument [application] cannot be null!");

        this.resolver = CachingPathMatchingResourcePatternResolver.INSTANCE;
        try {
            this.pluginResources = this.resolver.getResources(resourcePath);
        }
        catch (IOException ioe) {
            logger.error("Unable to load plugins for resource path " + resourcePath, ioe);
        }
        this.application = application;
        setPluginFilter();
    }

    public DefaultGrailsPluginManager(String[] pluginResources, GrailsApplication application) {
        super(application);
        this.resolver = CachingPathMatchingResourcePatternResolver.INSTANCE;

        List<Resource> resourceList = new ArrayList<>();
        for (String resourcePath : pluginResources) {
            try {
                resourceList.addAll(Arrays.asList(this.resolver.getResources(resourcePath)));
            }
            catch (IOException ioe) {
                logger.error("Unable to load plugins for resource path " + resourcePath, ioe);
            }
        }

        this.pluginResources = resourceList.toArray(new Resource[0]);
        this.application = application;
        setPluginFilter();
    }

    public DefaultGrailsPluginManager(Class<?>[] plugins, GrailsApplication application) {
        super(application);
        this.pluginClasses.addAll(Set.of(plugins));
        this.resolver = CachingPathMatchingResourcePatternResolver.INSTANCE;
        this.application = application;
        setPluginFilter();
    }

    public DefaultGrailsPluginManager(Resource[] pluginFiles, GrailsApplication application) {
        super(application);
        this.resolver = CachingPathMatchingResourcePatternResolver.INSTANCE;
        this.pluginResources = pluginFiles;
        this.application = application;
        setPluginFilter();
    }

    public DefaultGrailsPluginManager(GrailsApplication application) {
        super(application);
    }

    public GrailsPlugin[] getUserPlugins() {
        return this.userPlugins.toArray(new GrailsPlugin[0]);
    }

    private void setPluginFilter() {
        this.pluginFilter = new PluginFilterRetriever().getPluginFilter(this.application.getConfig());
    }

    public void refreshPlugin(String name) {
        if (hasGrailsPlugin(name)) {
            getGrailsPlugin(name).refresh();
        }
    }

    public Collection<GrailsPlugin> getPluginObservers(GrailsPlugin plugin) {
        Assert.notNull(plugin, "Argument [plugin] cannot be null");

        Collection<GrailsPlugin> c = this.pluginToObserverMap.get(plugin.getName());

        // Add any wildcard observers.
        Collection<GrailsPlugin> wildcardObservers = this.pluginToObserverMap.get("*");
        if (wildcardObservers != null) {
            if (c != null) {
                c.addAll(wildcardObservers);
            }
            else {
                c = wildcardObservers;
            }
        }

        if (c != null) {
            // Make sure this plugin is not observing itself!
            c.remove(plugin);
            return c;
        }

        return Collections.emptySet();
    }

    public void informObservers(String pluginName, Map<String, Object> event) {
        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        if (plugin == null) {
            return;
        }
        if (!plugin.isEnabled(this.applicationContext.getEnvironment().getActiveProfiles())) {
            return;
        }

        for (GrailsPlugin observingPlugin : getPluginObservers(plugin)) {
            if (!observingPlugin.isEnabled(this.applicationContext.getEnvironment().getActiveProfiles())) {
                continue;
            }

            observingPlugin.notifyOfEvent(event);
        }
    }

    /* (non-Javadoc)
     * @see grails.plugins.GrailsPluginManager#loadPlugins()
     */
    public void loadPlugins() throws PluginException {
        if (this.initialised) {
            return;
        }

        long time = System.currentTimeMillis();
        StartupStep pluginStep = getApplicationStartup().start("grails.plugins.loading");
        ClassLoader gcl = this.application.getClassLoader();

        attemptLoadPlugins(gcl);

        if (!this.delayedLoadPlugins.isEmpty()) {
            loadDelayedPlugins();
        }
        if (!this.delayedEvictions.isEmpty()) {
            processDelayedEvictions();
        }

        loadedPlugins = sortPlugins(loadedPlugins);
        initializePlugins();
        this.initialised = true;

        if (logger.isInfoEnabled()) {
            logger.info(String.format("Total %d plugins loaded successfully, take in %d ms", this.loadedPlugins.size(),
                    (System.currentTimeMillis() - time)));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Grails plugins in total: %d, core: %d, user: %d, loaded: %d, failed: %d, unenabled: %d",
                    this.allPlugins.size(), this.corePlugins.size(), this.userPlugins.size(),
                    this.allPlugins.size() - this.failedPlugins.size() - this.unenabledPlugins.size(),
                    this.failedPlugins.size(), this.unenabledPlugins.size()));
        }

        pluginStep.tag("pluginCount", String.valueOf(this.loadedPlugins.size())).end();
    }

    protected List<GrailsPlugin> sortPlugins(List<GrailsPlugin> toSort) {
        /* http://en.wikipedia.org/wiki/Topological_sorting
         *
        * L ← Empty list that will contain the sorted nodes
         S ← Set of all nodes

        function visit(node n)
            if n has not been visited yet then
                mark n as visited
                for each node m with an edge from n to m do
                    visit(m)
                add n to L

        for each node n in S do
            visit(n)

         */
        List<GrailsPlugin> sortedPlugins = new ArrayList<>(toSort.size());
        Set<GrailsPlugin> visitedPlugins = new HashSet<>();
        Map<GrailsPlugin, List<GrailsPlugin>> loadOrderDependencies = resolveLoadDependencies(toSort);

        for (GrailsPlugin plugin : toSort) {
            visitTopologicalSort(plugin, sortedPlugins, visitedPlugins, loadOrderDependencies);
        }

        return sortedPlugins;
    }

    protected Map<GrailsPlugin, List<GrailsPlugin>> resolveLoadDependencies(List<GrailsPlugin> plugins) {
        Map<GrailsPlugin, List<GrailsPlugin>> loadOrderDependencies = new HashMap<>();

        for (GrailsPlugin plugin : plugins) {
            if (plugin.getLoadAfterNames() != null) {
                List<GrailsPlugin> loadDepsForPlugin = loadOrderDependencies.computeIfAbsent(plugin, k -> new ArrayList<>());
                for (String pluginName : plugin.getLoadAfterNames()) {
                    GrailsPlugin loadAfterPlugin = getGrailsPlugin(pluginName);
                    if (loadAfterPlugin != null) {
                        loadDepsForPlugin.add(loadAfterPlugin);
                    }
                }
            }
            for (String loadBefore : plugin.getLoadBeforeNames()) {
                GrailsPlugin loadBeforePlugin = getGrailsPlugin(loadBefore);
                if (loadBeforePlugin != null) {
                    List<GrailsPlugin> loadDepsForPlugin = loadOrderDependencies.computeIfAbsent(loadBeforePlugin, k -> new ArrayList<>());
                    loadDepsForPlugin.add(plugin);
                }
            }
        }
        return loadOrderDependencies;
    }

    private void visitTopologicalSort(GrailsPlugin plugin, List<GrailsPlugin> sortedPlugins,
            Set<GrailsPlugin> visitedPlugins, Map<GrailsPlugin, List<GrailsPlugin>> loadOrderDependencies) {
        if (plugin != null && !visitedPlugins.contains(plugin)) {
            visitedPlugins.add(plugin);
            List<GrailsPlugin> loadDepsForPlugin = loadOrderDependencies.get(plugin);
            if (loadDepsForPlugin != null) {
                for (GrailsPlugin dependentPlugin : loadDepsForPlugin) {
                    visitTopologicalSort(dependentPlugin, sortedPlugins, visitedPlugins, loadOrderDependencies);
                }
            }
            sortedPlugins.add(plugin);
        }
    }

    private void attemptLoadPlugins(ClassLoader gcl) {
        // retrieve load core plugins first
        List<GrailsPlugin> grailsCorePlugins = this.loadCorePlugins ? findCorePlugins() : new ArrayList<>();
        this.corePlugins = grailsCorePlugins;

        List<GrailsPlugin> grailsUserPlugins = findUserPlugins(gcl);
        this.userPlugins = grailsUserPlugins;

        List<GrailsPlugin> grailsAllPlugins = new ArrayList<>(grailsCorePlugins);
        grailsAllPlugins.addAll(grailsUserPlugins);

        List<GrailsPlugin> filteredPlugins = getPluginFilter().filterPluginList(grailsAllPlugins);

        //make sure core plugins are loaded first
        List<GrailsPlugin> orderedCorePlugins = new ArrayList<>();
        List<GrailsPlugin> orderedUserPlugins = new ArrayList<>();

        for (GrailsPlugin plugin : filteredPlugins) {
            if (grailsCorePlugins.contains(plugin)) {
                orderedCorePlugins.add(plugin);
            }
            else {
                orderedUserPlugins.add(plugin);
            }
        }

        List<GrailsPlugin> orderedPlugins = new ArrayList<>();
        orderedPlugins.addAll(orderedCorePlugins);
        orderedPlugins.addAll(orderedUserPlugins);

        this.allPlugins.addAll(orderedPlugins);
        OrderComparator.sort(this.allPlugins);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Attempting to load [%d] core plugins and [%d] user defined plugins...",
                    this.corePlugins.size(), this.userPlugins.size()));
        }

        for (GrailsPlugin plugin : this.allPlugins) {
            attemptPluginLoad(plugin);
        }
    }

    private List<GrailsPlugin> findCorePlugins() {
        CorePluginFinder finder = new CorePluginFinder(this.application);
        finder.setParentApplicationContext(this.parentCtx);

        List<GrailsPlugin> grailsCorePlugins = new ArrayList<>();

        Class<?>[] corePluginClasses = finder.getPluginClasses();

        if (logger.isDebugEnabled()) {
            logger.debug("Found [" + corePluginClasses.length + "] core plugins to load...");
        }

        for (Class<?> pluginClass : corePluginClasses) {
            if (pluginClass != null && !Modifier.isAbstract(pluginClass.getModifiers()) && pluginClass != DefaultGrailsPlugin.class) {
                StartupStep pluginStep = getApplicationStartup().start("grails.plugins.instantiate");
                BinaryGrailsPluginDescriptor binaryDescriptor = finder.getBinaryDescriptor(pluginClass);
                GrailsPlugin plugin;
                if (binaryDescriptor != null) {
                    plugin = createBinaryGrailsPlugin(pluginClass, binaryDescriptor);
                }
                else {
                    plugin = createGrailsPlugin(pluginClass);
                }
                plugin.setApplicationContext(this.applicationContext);

                isCompatiblePlugin(plugin);

                grailsCorePlugins.add(plugin);
                pluginStep.tag("pluginName", plugin.getName())
                        .tag("pluginClass", plugin.getPluginClass().getName())
                        .end();
            }
        }
        return grailsCorePlugins;
    }

    private String getPluginGrailsVersion(GrailsPlugin plugin) {
        Object grailsVersionValue = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin.getInstance(), GRAILS_VERSION);
        return grailsVersionValue != null ? grailsVersionValue.toString() : null;
    }

    /**
     * Checks plugin compatibility against used Grails version
     *
     * @param plugin the plugin to check
     * @return true only in case plugin is compatible or impossible to determine, false otherwise
     */
    private boolean isCompatiblePlugin(GrailsPlugin plugin) {
        String pluginGrailsVersion = getPluginGrailsVersion(plugin);

        if (pluginGrailsVersion == null || pluginGrailsVersion.contains("@")) {
            if (logger.isDebugEnabled()) {
                logger.debug("Plugin grailsVersion is null or containing '@'. Compatibility check skipped.");
            }
            return true;
        }

        String appGrailsVersion = this.application.getMetadata().getGrailsVersion();
        String pluginMinGrailsVersion = GrailsVersionUtils.getLowerVersion(pluginGrailsVersion);
        String pluginMaxGrailsVersion = GrailsVersionUtils.getUpperVersion(pluginGrailsVersion);

        if (appGrailsVersion == null) {
            return true;
        }

        if (pluginMinGrailsVersion.equals("*")) {
            if (logger.isDebugEnabled()) {
                logger.debug("grailsVersion not formatted as expected, unable to determine compatibility.");
            }
            return false;
        }

        VersionComparator comparator = new VersionComparator();

        if (pluginMinGrailsVersion.equals(pluginMaxGrailsVersion)) {
            //exact version compatibility required
            if (!appGrailsVersion.equals(pluginMinGrailsVersion)) {
                logger.warn("Plugin [" + plugin.getName() + ":" + plugin.getVersion() +
                        "] may not be compatible with this application as the application Grails version is not equal" +
                        " to the one that plugin requires. Plugin is compatible with Grails version " +
                        pluginGrailsVersion + " but app is " + appGrailsVersion);
                return false;
            }
        }
        if (!pluginMaxGrailsVersion.equals("*")) {
            // Case 1: max version not specified. Forward compatibility expected

            // minimum version required by plugin cannot be greater than grails app version
            if (comparator.compare(pluginMinGrailsVersion, appGrailsVersion) > 0) {
                logger.warn("Plugin [" + plugin.getName() + ":" + plugin.getVersion() +
                        "] may not be compatible with this application as the application Grails version is less" +
                        " than the plugin requires. Plugin is compatible with Grails version " +
                        pluginGrailsVersion + " but app is " + appGrailsVersion);
                return false;
            }
        }
        else {
            // Case 2: both max and min version specified. Strict compatibility expected

            // minimum version required by plugin cannot be greater than grails app version
            if (comparator.compare(pluginMinGrailsVersion, appGrailsVersion) > 0) {
                logger.warn("Plugin [" + plugin.getName() + ":" + plugin.getVersion() +
                        "] may not be compatible with this application as the application Grails version is less" +
                        " than the plugin requires. Plugin is compatible with Grails version " +
                        pluginGrailsVersion + " but app is " + appGrailsVersion);
                return false;
            }

            // maximum version required by plugin cannot be less than grails app version
            if (comparator.compare(pluginMaxGrailsVersion, appGrailsVersion) < 0) {
                logger.warn("Plugin [" + plugin.getName() + ":" + plugin.getVersion() +
                        "] may not be compatible with this application as the application Grails version is greater" +
                        " than the plugins max specified. Plugin is compatible with Grails versions " +
                        pluginGrailsVersion + " but app is " + appGrailsVersion);
                return false;
            }
        }

        return true;
    }

    private GrailsPlugin createBinaryGrailsPlugin(Class<?> pluginClass, BinaryGrailsPluginDescriptor binaryDescriptor) {
        if (DynamicPlugin.class.isAssignableFrom(pluginClass)) {
            DynamicBinaryGrailsPlugin dynamicGrailsPlugin = new DynamicBinaryGrailsPlugin(pluginClass, binaryDescriptor, this.application);
            dynamicGrailsPlugin.setModuleDescriptorFactory(moduleDescriptorFactory);
            return dynamicGrailsPlugin;
        }
        return new BinaryGrailsPlugin(pluginClass, binaryDescriptor, this.application);
    }

    protected GrailsPlugin createGrailsPlugin(Class<?> pluginClass) {
        if (DynamicPlugin.class.isAssignableFrom(pluginClass)) {
            DefaultDynamicGrailsPlugin dynamicGrailsPlugin = new DefaultDynamicGrailsPlugin(pluginClass, this.application);
            dynamicGrailsPlugin.setModuleDescriptorFactory(moduleDescriptorFactory);
            return dynamicGrailsPlugin;
        }
        return new DefaultGrailsPlugin(pluginClass, this.application);
    }

    protected GrailsPlugin createGrailsPlugin(Class<?> pluginClass, Resource resource) {
        return new DefaultGrailsPlugin(pluginClass, resource, this.application);
    }

    private List<GrailsPlugin> findUserPlugins(ClassLoader gcl) {
        List<GrailsPlugin> grailsUserPlugins = new ArrayList<>();

        if (logger.isDebugEnabled()) {
            int totalUserPlugins = this.pluginResources.length + this.pluginClasses.size();
            logger.debug("Found [" + totalUserPlugins + "] user defined plugins to load...");
        }
        for (Resource r : this.pluginResources) {
            Class<?> pluginClass = loadPluginClass(gcl, r);

            if (isGrailsPlugin(pluginClass)) {
                GrailsPlugin plugin = createGrailsPlugin(pluginClass, r);
                //attemptPluginLoad(plugin);
                isCompatiblePlugin(plugin);
                grailsUserPlugins.add(plugin);
            }
            else {
                logger.warn("Class [" + pluginClass + "] not loaded as plugin. Grails plugins must end with the convention 'GrailsPlugin'!");
            }
        }

        for (Class<?> pluginClass : this.pluginClasses) {
            if (isGrailsPlugin(pluginClass)) {
                GrailsPlugin plugin = createGrailsPlugin(pluginClass);
                //attemptPluginLoad(plugin);
                isCompatiblePlugin(plugin);
                grailsUserPlugins.add(plugin);
            }
            else {
                logger.warn("Class [" + pluginClass + "] not loaded as plugin. Grails plugins must end with the convention 'GrailsPlugin'!");
            }
        }
        return grailsUserPlugins;
    }

    private boolean isGrailsPlugin(Class<?> pluginClass) {
        return pluginClass != null && pluginClass.getName().endsWith(GRAILS_PLUGIN_SUFFIX);
    }

    private void processDelayedEvictions() {
        for (Map.Entry<GrailsPlugin, String[]> entry : this.delayedEvictions.entrySet()) {
            GrailsPlugin plugin = entry.getKey();
            for (String pluginName : entry.getValue()) {
                evictPlugin(plugin, pluginName);
            }
        }
    }

    private void initializePlugins() {
        for (GrailsPlugin plugin : this.plugins.values()) {
            if (plugin != null) {
                plugin.setApplicationContext(this.applicationContext);
            }
        }
    }

    /**
     * This method will attempt to load that plugins not loaded in the first pass
     */
    private void loadDelayedPlugins() {
        while (!this.delayedLoadPlugins.isEmpty()) {
            GrailsPlugin plugin = this.delayedLoadPlugins.remove(0);
            if (areDependenciesResolved(plugin)) {
                if (!hasValidPluginsToLoadBefore(plugin)) {
                    registerPlugin(plugin);
                }
                else {
                    this.delayedLoadPlugins.add(plugin);
                }
            }
            else {
                // ok, it still hasn't resolved the dependency after the initial
                // load of all plugins. All hope is not lost, however, so lets first
                // look inside the remaining delayed loads before giving up
                boolean foundInDelayed = false;
                for (GrailsPlugin remainingPlugin : this.delayedLoadPlugins) {
                    if (isDependentOn(plugin, remainingPlugin)) {
                        foundInDelayed = true;
                        break;
                    }
                }
                if (foundInDelayed) {
                    this.delayedLoadPlugins.add(plugin);
                }
                else {
                    this.failedPlugins.put(plugin.getName(), plugin);
                    this.allPluginsOrdered.add(plugin);
                    logger.error("Plugin [" + plugin.getName() + "] with version [" +
                            plugin.getVersion() + "] cannot be loaded, because its dependencies " +
                            DefaultGroovyMethods.inspect(plugin.getDependencyNames()) + " cannot be resolved");
                }
            }
        }
    }

    private boolean hasValidPluginsToLoadBefore(GrailsPlugin plugin) {
        String[] loadAfterNames = plugin.getLoadAfterNames();
        for (GrailsPlugin delayedLoadPlugin : this.delayedLoadPlugins) {
            for (String name : loadAfterNames) {
                if (delayedLoadPlugin.getName().equals(name)) {
                    return hasDelayedDependencies(delayedLoadPlugin) || areDependenciesResolved(delayedLoadPlugin);
                }
            }
        }
        return false;
    }

    private boolean hasDelayedDependencies(GrailsPlugin other) {
        String[] dependencyNames = other.getDependencyNames();
        for (String dependencyName : dependencyNames) {
            for (GrailsPlugin grailsPlugin : this.delayedLoadPlugins) {
                if (grailsPlugin.getName().equals(dependencyName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the first plugin is dependant on the second plugin.
     *
     * @param plugin     The plugin to check
     * @param dependency The plugin which the first argument may be dependant on
     * @return true if it is
     */
    private boolean isDependentOn(GrailsPlugin plugin, GrailsPlugin dependency) {
        for (String name : plugin.getDependencyNames()) {
            String requiredVersion = plugin.getDependentVersion(name);

            if (name.equals(dependency.getName()) &&
                    GrailsVersionUtils.isValidVersion(dependency.getVersion(), requiredVersion)) {
                return true;
            }
        }
        return false;
    }

    private boolean areDependenciesResolved(GrailsPlugin plugin) {
        for (String name : plugin.getDependencyNames()) {
            if (!hasGrailsPlugin(name, plugin.getDependentVersion(name))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if there are no plugins left that should, if possible, be loaded before this plugin.
     *
     * @param plugin The plugin
     * @return true if there are
     */
    private boolean areNoneToLoadBefore(GrailsPlugin plugin) {
        for (String name : plugin.getLoadAfterNames()) {
            if (getGrailsPlugin(name) == null) {
                return false;
            }
        }
        return true;
    }

    private Class<?> loadPluginClass(ClassLoader cl, Resource r) {
        Class<?> pluginClass;
        if (cl instanceof GroovyClassLoader) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Parsing & compiling " + r.getFilename());
                }
                pluginClass = ((GroovyClassLoader) cl).parseClass(IOGroovyMethods.getText(r.getInputStream(), "UTF-8"));
            }
            catch (CompilationFailedException e) {
                throw new PluginException("Error compiling plugin [" + r.getFilename() + "] " + e.getMessage(), e);
            }
            catch (IOException e) {
                throw new PluginException("Error reading plugin [" + r.getFilename() + "] " + e.getMessage(), e);
            }
        }
        else {
            String className;
            try {
                className = GrailsResourceUtils.getClassName(r.getFile().getAbsolutePath());
            }
            catch (IOException e) {
                throw new PluginException("Cannot find plugin class from resource: [" + r.getFilename() + "]", e);
            }
            try {
                pluginClass = Class.forName(className, true, cl);
            }
            catch (ClassNotFoundException e) {
                throw new PluginException("Cannot find plugin class [" + className + "] resource: [" + r.getFilename() + "]", e);
            }
        }
        return pluginClass;
    }

    /**
     * Attempts to load a plugin based on its dependencies. If a plugin's dependencies cannot be resolved
     * it will add it to the list of dependencies to be resolved later.
     *
     * @param plugin The plugin
     */
    private void attemptPluginLoad(GrailsPlugin plugin) {
        if (areDependenciesResolved(plugin) && areNoneToLoadBefore(plugin)) {
            registerPlugin(plugin);
        }
        else {
            this.delayedLoadPlugins.add(plugin);
        }
    }

    private void registerPlugin(GrailsPlugin plugin) {
        StartupStep pluginStep = getApplicationStartup().start("grails.plugins.loaded")
                .tag("pluginName", plugin.getName())
                .tag("pluginClass", plugin.getPluginClass().getName());
        if (!canRegisterPlugin(plugin)) {
            String message = "Plugin [" + plugin.getName() + "] with version [" + plugin.getVersion() + "] is disabled and was not loaded";
            if (logger.isDebugEnabled()) {
                logger.debug(message);
            }
            this.unenabledPlugins.add(plugin);
            this.allPluginsOrdered.add(plugin);
            pluginStep.tag("message", message).end();
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Plugin [" + plugin.getName() + "] with version [" + plugin.getVersion() + "] loaded successfully");
        }

        if (plugin instanceof ParentApplicationContextAware) {
            ((ParentApplicationContextAware) plugin).setParentApplicationContext(this.parentCtx);
        }
        plugin.setManager(this);
        String[] evictionNames = plugin.getEvictionNames();
        if (evictionNames.length > 0) {
            this.delayedEvictions.put(plugin, evictionNames);
        }

        String[] observedPlugins = plugin.getObservedPluginNames();
        for (String observedPlugin : observedPlugins) {
            Set<GrailsPlugin> observers = this.pluginToObserverMap.computeIfAbsent(observedPlugin, k -> new HashSet<>());
            observers.add(plugin);
        }
        this.loadedPlugins.add(plugin);
        this.allPluginsOrdered.add(plugin);
        this.plugins.put(plugin.getName(), plugin);
        this.classNameToPluginMap.put(plugin.getPluginClass().getName(), plugin);
        pluginStep.end();
    }

    protected boolean canRegisterPlugin(GrailsPlugin plugin) {
        Environment environment = Environment.getCurrent();
        return plugin.isEnabled() && plugin.supportsEnvironment(environment);
    }

    protected void evictPlugin(GrailsPlugin evictor, String evicteeName) {
        GrailsPlugin pluginToEvict = this.plugins.get(evicteeName);
        if (pluginToEvict != null) {
            this.loadedPlugins.remove(pluginToEvict);
            this.plugins.remove(pluginToEvict.getName());

            if (logger.isWarnEnabled()) {
                logger.warn("Plugin " + pluginToEvict + " was evicted by " + evictor);
            }
        }
    }

    private boolean hasGrailsPlugin(String name, String version) {
        return getGrailsPlugin(name, version) != null;
    }

    public void setParentApplicationContext(ApplicationContext parent) {
        this.parentCtx = parent;
    }

    /**
     * @deprecated Replaced by agent-based reloading, will be removed in a future version of Grails
     */
    @Deprecated
    public void checkForChanges() {
        // do nothing
    }

    public void reloadPlugin(GrailsPlugin plugin) {
        plugin.doArtefactConfiguration();

        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(this.parentCtx);

        doRuntimeConfiguration(plugin.getName(), springConfig);
        springConfig.registerBeansWithContext((GenericApplicationContext) this.applicationContext);

        plugin.doWithApplicationContext(this.applicationContext);
        plugin.doWithDynamicMethods(this.applicationContext);
    }

    @Override
    public void setApplication(GrailsApplication application) {
        Assert.notNull(application, "Argument [application] cannot be null");
        this.application = application;
        for (GrailsPlugin plugin : this.loadedPlugins) {
            plugin.setApplication(application);
        }
    }

    @Override
    public void doDynamicMethods() {
        checkInitialised();
        // remove common meta classes just to be sure
        MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
        for (Class<?> COMMON_CLASS : COMMON_CLASSES) {
            registry.removeMetaClass(COMMON_CLASS);
        }
        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                try {
                    plugin.doWithDynamicMethods(this.applicationContext);
                }
                catch (Throwable t) {
                    throw new GrailsConfigurationException("Error configuring dynamic methods for plugin " + plugin + ": " + t.getMessage(), t);
                }
            }
        }
    }

    private PluginFilter getPluginFilter() {
        if (this.pluginFilter == null) {
            this.pluginFilter = new IdentityPluginFilter();
        }
        return this.pluginFilter;
    }

    public void setPluginFilter(PluginFilter pluginFilter) {
        this.pluginFilter = pluginFilter;
    }

    public List<GrailsPlugin> getPluginList() {
        return Collections.unmodifiableList(this.allPluginsOrdered);
    }

}
