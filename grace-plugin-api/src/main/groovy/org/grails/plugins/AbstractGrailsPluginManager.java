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
package org.grails.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.util.Assert;

import grails.artefact.Enhanced;
import grails.core.ArtefactHandler;
import grails.core.GrailsApplication;
import grails.plugins.DynamicGrailsPlugin;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.plugins.GrailsVersionUtils;
import grails.plugins.ModuleDescriptor;
import grails.plugins.ModuleDescriptorFactory;
import grails.plugins.Plugin;
import grails.plugins.PluginFilter;
import grails.plugins.exceptions.PluginException;
import grails.plugins.module.ModuleType;
import grails.util.BuildSettings;
import grails.util.Environment;
import grails.util.GrailsNameUtils;

import org.grails.config.NavigableMap;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.support.WatchPattern;
import org.grails.spring.RuntimeSpringConfiguration;

/**
 * Abstract implementation of the GrailsPluginManager interface
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.4
 */
public abstract class AbstractGrailsPluginManager implements GrailsPluginManager {

    private static final Log logger = LogFactory.getLog(AbstractGrailsPluginManager.class);

    private static final String BLANK = "";

    public static final String CONFIG_FILE = "application.groovy";

    protected List<GrailsPlugin> loadedPlugins = new ArrayList<>();

    protected GrailsApplication application;

    protected Resource[] pluginResources = new Resource[0];

    protected Map<String, GrailsPlugin> plugins = new HashMap<>();

    protected Map<String, GrailsPlugin> classNameToPluginMap = new HashMap<>();

    protected Set<Class<?>> pluginClasses = new HashSet<>();

    protected boolean initialised = false;

    protected boolean shutdown = false;

    protected ApplicationContext applicationContext;

    protected ModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();

    /**
     * Application startup metrics.
     **/
    private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

    protected Map<String, GrailsPlugin> failedPlugins = new HashMap<>();

    protected boolean loadCorePlugins = true;

    private static final String CONFIG_BINDING_USER_HOME = "userHome";

    private static final String CONFIG_BINDING_GRAILS_HOME = "grailsHome";

    private static final String CONFIG_BINDING_APP_NAME = "appName";

    private static final String CONFIG_BINDING_APP_VERSION = "appVersion";

    public AbstractGrailsPluginManager(GrailsApplication application) {
        Assert.notNull(application, "Argument [application] cannot be null!");
        this.application = application;
    }

    public GrailsPlugin[] getAllPlugins() {
        return this.loadedPlugins.toArray(new GrailsPlugin[0]);
    }

    public GrailsPlugin[] getFailedLoadPlugins() {
        return this.failedPlugins.values().toArray(new GrailsPlugin[0]);
    }

    /**
     * @return the initialised
     */
    public boolean isInitialised() {
        return this.initialised;
    }

    protected void checkInitialised() {
        Assert.state(this.initialised, "Must call loadPlugins() before invoking configurational methods on GrailsPluginManager");
    }

    @Override
    public void setApplicationStartup(ApplicationStartup applicationStartup) {
        Assert.notNull(applicationStartup, "applicationStartup should not be null");
        this.applicationStartup = applicationStartup;
    }

    @Override
    public ApplicationStartup getApplicationStartup() {
        return this.applicationStartup;
    }

    public GrailsPlugin getFailedPlugin(String name) {
        if (name.contains("-")) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        return this.failedPlugins.get(name);
    }

    /**
     * Base implementation that simply goes through the list of plugins and calls doWithRuntimeConfiguration on each
     *
     * @param springConfig The RuntimeSpringConfiguration instance
     */
    public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
        ApplicationContext context = springConfig.getUnrefreshedApplicationContext();
        AutowireCapableBeanFactory autowireCapableBeanFactory = context.getAutowireCapableBeanFactory();

        if (autowireCapableBeanFactory instanceof ConfigurableListableBeanFactory) {
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) autowireCapableBeanFactory;
            ConversionService existingConversionService = beanFactory.getConversionService();
            ConverterRegistry converterRegistry;
            if (existingConversionService == null) {
                GenericConversionService conversionService = new GenericConversionService();
                converterRegistry = conversionService;
                beanFactory.setConversionService(conversionService);
            }
            else {
                converterRegistry = (ConverterRegistry) existingConversionService;
            }

            converterRegistry.addConverter(new Converter<NavigableMap.NullSafeNavigator, Object>() {
                @Override
                public Object convert(NavigableMap.NullSafeNavigator source) {
                    return null;
                }
            });
        }

        checkInitialised();

        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (plugin.supportsCurrentScopeAndEnvironment() && plugin.isEnabled(context.getEnvironment().getActiveProfiles())) {
                plugin.doWithRuntimeConfiguration(springConfig);
            }
        }
    }

    /**
     * Base implementation that will perform runtime configuration for the specified plugin name.
     */
    public void doRuntimeConfiguration(String pluginName, RuntimeSpringConfiguration springConfig) {
        checkInitialised();

        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        if (plugin == null) {
            throw new PluginException("Plugin [" + pluginName + "] not found");
        }

        if (!plugin.supportsCurrentScopeAndEnvironment()) {
            return;
        }

        if (!plugin.isEnabled(this.applicationContext.getEnvironment().getActiveProfiles())) {
            return;
        }

        String[] dependencyNames = plugin.getDependencyNames();
        doRuntimeConfigurationForDependencies(dependencyNames, springConfig);

        String[] loadAfters = plugin.getLoadAfterNames();
        for (String name : loadAfters) {
            GrailsPlugin current = getGrailsPlugin(name);
            if (current != null) {
                current.doWithRuntimeConfiguration(springConfig);
            }
        }
        plugin.doWithRuntimeConfiguration(springConfig);
    }

    private void doRuntimeConfigurationForDependencies(String[] dependencyNames, RuntimeSpringConfiguration springConfig) {
        for (String dn : dependencyNames) {
            GrailsPlugin current = getGrailsPlugin(dn);
            if (current == null) {
                throw new PluginException("Cannot load Plugin. Dependency [" + dn + "] not found");
            }

            String[] pluginDependencies = current.getDependencyNames();
            if (pluginDependencies.length > 0) {
                doRuntimeConfigurationForDependencies(pluginDependencies, springConfig);
            }
            if (isPluginDisabledForProfile(current)) {
                continue;
            }
            current.doWithRuntimeConfiguration(springConfig);
        }
    }

    /**
     * Base implementation that will simply go through each plugin and call doWithApplicationContext on each.
     */
    public void doPostProcessing(ApplicationContext ctx) {
        checkInitialised();

        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (isPluginDisabledForProfile(plugin)) {
                continue;
            }
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doWithApplicationContext(ctx);
            }
        }
    }

    public Resource[] getPluginResources() {
        return this.pluginResources;
    }

    public GrailsPlugin getGrailsPlugin(String name) {
        if (name.contains("-")) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        return this.plugins.get(name);
    }

    public GrailsPlugin getGrailsPluginForClassName(String name) {
        return this.classNameToPluginMap.get(name);
    }

    public GrailsPlugin getGrailsPlugin(String name, Object version) {
        if (name.contains("-")) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }

        GrailsPlugin plugin = this.plugins.get(name);
        if (plugin != null && GrailsVersionUtils.isValidVersion(plugin.getVersion(), version.toString())) {
            return plugin;
        }
        return null;
    }

    public boolean hasGrailsPlugin(String name) {
        if (name.indexOf('-') > -1) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        return this.plugins.containsKey(name);
    }

    public void doDynamicMethods() {
        checkInitialised();
        Class<?>[] allClasses = this.application.getAllClasses();
        if (allClasses != null) {
            for (Class<?> c : allClasses) {
                ExpandoMetaClass emc = new ExpandoMetaClass(c, true, true);
                emc.initialize();
            }
            ApplicationContext ctx = this.applicationContext;
            for (GrailsPlugin plugin : this.loadedPlugins) {
                if (!plugin.isEnabled(ctx.getEnvironment().getActiveProfiles())) {
                    continue;
                }
                plugin.doWithDynamicMethods(ctx);
            }
        }
    }

    public void doDynamicModules() {
        checkInitialised();

        ApplicationContext ctx = this.applicationContext;
        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (!plugin.isEnabled(ctx.getEnvironment().getActiveProfiles())) {
                continue;
            }
            if (plugin instanceof DynamicGrailsPlugin) {
                DynamicGrailsPlugin dynamicPlugin = (DynamicGrailsPlugin) plugin;
                dynamicPlugin.doWithDynamicModules();
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.moduleDescriptorFactory.setApplicationContext(applicationContext);
        for (GrailsPlugin plugin : this.loadedPlugins) {
            plugin.setApplicationContext(applicationContext);
        }
    }

    public void setApplication(GrailsApplication application) {
        Assert.notNull(application, "Argument [application] cannot be null");
        this.application = application;

        for (GrailsPlugin plugin : this.loadedPlugins) {
            plugin.setApplication(application);
        }
    }

    public GrailsApplication getApplication() {
        return this.application;
    }

    public void registerProvidedArtefacts(GrailsApplication app) {
        checkInitialised();

        // since plugin classes are added as overridable artefacts, which are added as the first
        // item in the list of artefacts, we have to iterate in reverse order to ensure plugin
        // load sequence is maintained
        ArrayList<GrailsPlugin> plugins = new ArrayList<>(this.loadedPlugins);
        Collections.reverse(plugins);

        for (GrailsPlugin plugin : plugins) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                if (isPluginDisabledForProfile(plugin)) {
                    continue;
                }
                for (Class<?> artefact : plugin.getProvidedArtefacts()) {
                    String shortName = GrailsNameUtils.getShortName(artefact);
                    if (artefact.getName().equals(shortName)) {
                        logger.warn("Plugin " + plugin.getName() + " has an artefact " + shortName + " without a package name " +
                                "This could lead to artefacts being excluded from the application");
                        if (app.getClassForName(shortName) != null) {
                            logger.error("Plugin " + plugin.getName() + " has an artefact " + shortName + " that is being excluded from " +
                                    "the application because another artefact exists with the same name without a package defined.");
                        }
                    }
                    if (!isAlreadyRegistered(app, artefact)) {
                        app.addOverridableArtefact(artefact);
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerProvidedModules() {
        checkInitialised();

        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                if (isPluginDisabledForProfile(plugin)) {
                    continue;
                }
                if (!(plugin instanceof DynamicGrailsPlugin)) {
                    continue;
                }
                DynamicGrailsPlugin dynamicPlugin = (DynamicGrailsPlugin) plugin;
                Object providedModules = dynamicPlugin.getProvidedModules();
                if (providedModules instanceof Collection) {
                    Collection<Class<?>> moduleTypesCollection = (Collection<Class<?>>) providedModules;
                    for (Class<?> clazz : moduleTypesCollection) {
                        if (ModuleDescriptor.class.isAssignableFrom(clazz)) {
                            String type = null;
                            ModuleType moduleType = clazz.getAnnotation(ModuleType.class);
                            if (moduleType != null) {
                                type = moduleType.value();
                            }
                            if (StringUtils.isBlank(type)) {
                                String shortName = GrailsNameUtils.getShortName(clazz);
                                if (shortName.endsWith("ModuleDescriptor")) {
                                    type = StringUtils.uncapitalize(StringUtils.substringBefore(shortName, "ModuleDescriptor"));
                                }
                                else {
                                    type = StringUtils.uncapitalize(shortName);
                                }
                            }
                            this.moduleDescriptorFactory.addModuleDescriptor(type, (Class<? extends ModuleDescriptor<?>>) clazz);
                        }
                    }
                }
                else if (providedModules instanceof Map) {
                    Map<String, Class<?>> moduleTypesMap = (Map<String, Class<?>>) providedModules;
                    for (Map.Entry<String, Class<?>> moduleType : moduleTypesMap.entrySet()) {
                        if (ModuleDescriptor.class.isAssignableFrom(moduleType.getValue())) {
                            this.moduleDescriptorFactory.addModuleDescriptor(
                                    moduleType.getKey(),
                                    (Class<? extends ModuleDescriptor<?>>) moduleType.getValue());
                        }
                    }
                }
            }
        }
    }

    private boolean isAlreadyRegistered(GrailsApplication app, Class<?> artefact) {
        return app.getClassForName(artefact.getName()) != null;
    }

    public void doArtefactConfiguration() {
        checkInitialised();

        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (isPluginDisabledForProfile(plugin)) {
                continue;
            }
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doArtefactConfiguration();
            }
        }
    }

    protected boolean isPluginDisabledForProfile(GrailsPlugin plugin) {
        return this.applicationContext != null && !plugin.isEnabled(this.applicationContext.getEnvironment().getActiveProfiles());
    }

    public void onStartup(Map<String, Object> event) {
        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (plugin.getInstance() instanceof Plugin) {
                ((Plugin) plugin.getInstance()).onStartup(event);
            }
        }
    }

    public void shutdown() {
        checkInitialised();

        try {
            // Shutdown plugins in reverse dependency order
            List<GrailsPlugin> reversePluginList = new ArrayList<>(this.loadedPlugins);
            Collections.reverse(reversePluginList);

            for (GrailsPlugin plugin : reversePluginList) {
                if (!plugin.isEnabled(this.applicationContext.getEnvironment().getActiveProfiles())) {
                    continue;
                }
                if (plugin.supportsCurrentScopeAndEnvironment()) {
                    plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_SHUTDOWN, plugin);
                }
            }
        }
        finally {
            this.shutdown = true;
        }

    }

    public boolean isShutdown() {
        return this.shutdown;
    }

    @Override
    public void setPluginFilter(PluginFilter pluginFilter) {
        // no-op
    }

    public void setLoadCorePlugins(boolean shouldLoadCorePlugins) {
        this.loadCorePlugins = shouldLoadCorePlugins;
    }

    public void informOfClassChange(Class<?> aClass) {
        if (aClass == null || this.application == null) {
            return;
        }

        ArtefactHandler handler = this.application.getArtefactType(aClass);
        if (handler == null) {
            return;
        }

        String pluginName = handler.getPluginName();
        if (pluginName == null) {
            return;
        }

        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        if (plugin != null) {
            if (!plugin.isEnabled(this.applicationContext.getEnvironment().getActiveProfiles())) {
                return;
            }
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, aClass);
        }
        else {
            String classNameAsPath = aClass.getName().replace('.', File.separatorChar);
            String groovyClass = classNameAsPath + ".groovy";
            String javaClass = classNameAsPath + ".java";
            for (GrailsPlugin grailsPlugin : this.loadedPlugins) {
                List<WatchPattern> watchPatterns = grailsPlugin.getWatchedResourcePatterns();
                if (watchPatterns != null) {
                    for (WatchPattern watchPattern : watchPatterns) {
                        File parent = watchPattern.getDirectory();
                        String extension = watchPattern.getExtension();

                        if (parent != null && extension != null) {
                            File f = new File(parent, groovyClass);
                            if (f.exists() && f.getName().endsWith(extension)) {
                                grailsPlugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, aClass);
                            }
                            else {
                                f = new File(parent, javaClass);
                                if (f.exists() && f.getName().endsWith(extension)) {
                                    grailsPlugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, aClass);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public String getPluginPath(String name) {
        return getPluginPath(name, false);
    }

    public String getPluginPath(String name, boolean forceCamelCase) {
        GrailsPlugin plugin = getGrailsPlugin(name);

        if (plugin != null && !plugin.isBasePlugin()) {
            if (forceCamelCase) {
                return plugin.getPluginPathCamelCase();
            }
            else {
                return plugin.getPluginPath();
            }
        }
        return BLANK;
    }

    public String getPluginPathForInstance(Object instance) {
        if (instance != null) {
            return getPluginPathForClass(instance.getClass());
        }
        return null;
    }

    public GrailsPlugin getPluginForInstance(Object instance) {
        if (instance != null) {
            return getPluginForClass(instance.getClass());
        }
        return null;
    }

    public GrailsPlugin getPluginForClass(Class<?> theClass) {
        if (theClass != null) {
            grails.plugins.metadata.GrailsPlugin ann =
                    theClass.getAnnotation(grails.plugins.metadata.GrailsPlugin.class);
            if (ann != null) {
                return getGrailsPlugin(ann.name());
            }
        }
        return null;
    }

    @Override
    public void informPluginsOfConfigChange() {
        for (GrailsPlugin plugin : this.loadedPlugins) {
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, this.application.getConfig());
        }
    }

    public void informOfFileChange(File file) {
        String className = GrailsResourceUtils.getClassName(file.getAbsolutePath());
        Class<?> cls = null;
        if (className != null) {
            cls = loadApplicationClass(className);
        }
        informOfClassChange(file, cls);
    }

    static ConfigSlurper getConfigSlurper(GrailsApplication application) {
        String environment = Environment.getCurrent().getName();
        ConfigSlurper configSlurper = new ConfigSlurper(environment);
        Map<String, Object> binding = new HashMap<>();
        // configure config slurper binding
        binding.put(CONFIG_BINDING_USER_HOME, System.getProperty("user.home"));
        binding.put(CONFIG_BINDING_GRAILS_HOME, System.getProperty("grails.home"));
        if (application != null) {
            binding.put(CONFIG_BINDING_APP_NAME, application.getMetadata().getApplicationName());
            binding.put(CONFIG_BINDING_APP_VERSION, application.getMetadata().getApplicationVersion());
            binding.put(GrailsApplication.APPLICATION_ID, application);
        }
        configSlurper.setBinding(binding);
        return configSlurper;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void informOfClassChange(File file, Class<?> cls) {
        if (file.getName().equals(CONFIG_FILE)) {
            ConfigSlurper configSlurper = getConfigSlurper(this.application);
            ConfigObject c;
            try {
                c = configSlurper.parse(file.toURI().toURL());
                this.application.getConfig().merge(c);
                Map flat = c.flatten();
                this.application.getConfig().merge(flat);
                this.application.configChanged();
                informPluginsOfConfigChange();
            }
            catch (Exception e) {
                logger.debug("Error in changing Config", e);
            }
        }
        else {
            if (cls != null) {
                MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
                registry.removeMetaClass(cls);
                ExpandoMetaClass newMc = new ExpandoMetaClass(cls, true, true);
                newMc.initialize();
                registry.setMetaClass(cls, newMc);

                Enhanced en = AnnotationUtils.findAnnotation(cls, Enhanced.class);
                if (en != null) {
                    Class<?>[] mixinClasses = en.mixins();
                    if (mixinClasses != null) {
                        DefaultGroovyMethods.mixin(newMc, mixinClasses);
                    }
                }
            }

            for (GrailsPlugin grailsPlugin : this.loadedPlugins) {
                if (grailsPlugin.hasInterestInChange(file.getAbsolutePath())) {
                    try {
                        if (cls == null) {
                            grailsPlugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, new FileSystemResource(file));
                        }
                        else {
                            grailsPlugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, cls);
                        }
                        Environment.setCurrentReloadError(null);
                    }
                    catch (Exception e) {
                        logger.error("Plugin " + grailsPlugin + " could not reload changes to file [" + file + "]: " + e.getMessage(), e);
                        Environment.setCurrentReloadError(e);
                    }
                }
            }
        }
    }

    private Class<?> loadApplicationClass(String className) {
        Class<?> cls = null;
        try {
            cls = this.application.getClassLoader().loadClass(className);
        }
        catch (ClassNotFoundException ignored) {
        }
        return cls;
    }

    public String getPluginPathForClass(Class<?> theClass) {
        if (theClass != null) {
            grails.plugins.metadata.GrailsPlugin ann =
                    theClass.getAnnotation(grails.plugins.metadata.GrailsPlugin.class);
            if (ann != null) {
                return getPluginPath(ann.name());
            }
        }
        return null;
    }

    public String getPluginViewsPathForInstance(Object instance) {
        if (instance != null) {
            return getPluginViewsPathForClass(instance.getClass());
        }
        return null;
    }

    public String getPluginViewsPathForClass(Class<?> theClass) {
        if (theClass != null) {
            String path = getPluginPathForClass(theClass);
            if (StringUtils.isNotBlank(path)) {
                return path + "/" + BuildSettings.GRAILS_APP_PATH + "/" + "views";
            }
        }
        return null;
    }

    /**
     * Add User Plugin from Class
     *
     * @param pluginClass the class of Plugin
     * @since 2022.0.0
     */
    public void addUserPlugin(Class<?> pluginClass) {
        this.pluginClasses.add(pluginClass);
    }

    /**
     * Gets all module descriptors of installed modules.
     *
     * @since 2022.0.0
     */
    @Override
    public Collection<ModuleDescriptor<?>> getModuleDescriptors() {
        List<ModuleDescriptor<?>> moduleDescriptors = new LinkedList<>();
        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (plugin instanceof DynamicGrailsPlugin) {
                DynamicGrailsPlugin dynamicPlugin = (DynamicGrailsPlugin) plugin;
                moduleDescriptors.addAll(dynamicPlugin.getModuleDescriptors());
            }
        }
        return moduleDescriptors;
    }

    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(Predicate<ModuleDescriptor<M>> moduleDescriptorPredicate) {
        List<DynamicGrailsPlugin> dynamicPlugins = this.loadedPlugins.stream()
                .filter(plugin -> plugin instanceof DynamicGrailsPlugin)
                .map(plugin -> (DynamicGrailsPlugin) plugin).collect(Collectors.toList());

        return getModuleDescriptors(dynamicPlugins, moduleDescriptorPredicate)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @SuppressWarnings("unchecked")
    private <M> Stream<ModuleDescriptor<M>> getModuleDescriptors(Collection<DynamicGrailsPlugin> plugins,
            Predicate<ModuleDescriptor<M>> predicate) {

        return plugins.stream()
                .flatMap(plugin -> plugin.getModuleDescriptors().stream())
                .map(descriptor -> (ModuleDescriptor<M>) descriptor)
                .filter(predicate);
    }

    /**
     * Get all enabled module descriptors that have a specific descriptor class.
     *
     * @param descriptorClazz module descriptor class
     * @return List of {@link ModuleDescriptor}s that implement or extend the given class.
     * @since 2022.0.0
     */
    @Override
    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(Class<D> descriptorClazz) {
        List<D> result = new LinkedList<>();

        for (GrailsPlugin plugin : this.loadedPlugins) {
            if (plugin instanceof DynamicGrailsPlugin && plugin.isEnabled()) {
                DynamicGrailsPlugin dynamicPlugin = (DynamicGrailsPlugin) plugin;
                for (ModuleDescriptor<?> moduleDescriptor : dynamicPlugin.getModuleDescriptors()) {
                    if (descriptorClazz.isInstance(moduleDescriptor) && moduleDescriptor.isEnabled()) {
                        result.add(descriptorClazz.cast(moduleDescriptor));
                    }
                }
            }
        }

        return result;
    }

}
