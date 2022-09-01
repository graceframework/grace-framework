/*
 * Copyright 2004-2022 the original author or authors.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.TypeFilter;

import grails.core.ArtefactHandler;
import grails.core.GrailsApplication;
import grails.core.support.GrailsApplicationAware;
import grails.core.support.ParentApplicationContextAware;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.plugins.Plugin;
import grails.plugins.exceptions.PluginException;
import grails.spring.BeanBuilder;
import grails.util.CollectionUtils;
import grails.util.Environment;
import grails.util.GrailsArrayUtils;
import grails.util.GrailsClassUtils;
import grails.util.GrailsUtil;

import org.grails.core.io.CachingPathMatchingResourcePatternResolver;
import org.grails.core.io.SpringResource;
import org.grails.plugins.support.WatchPattern;
import org.grails.plugins.support.WatchPatternParser;
import org.grails.spring.RuntimeSpringConfiguration;

/**
 * Implementation of the GrailsPlugin interface that wraps a Groovy plugin class
 * and provides the magic to invoke its various methods from Java.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@SuppressWarnings("rawtypes")
public class DefaultGrailsPlugin extends AbstractGrailsPlugin implements ParentApplicationContextAware {

    protected static final Log logger = LogFactory.getLog(DefaultGrailsPlugin.class);

    private static final String PLUGIN_CHANGE_EVENT_CTX = "ctx";

    private static final String PLUGIN_CHANGE_EVENT_APPLICATION = "application";

    private static final String PLUGIN_CHANGE_EVENT_PLUGIN = "plugin";

    private static final String PLUGIN_CHANGE_EVENT_SOURCE = "source";

    private static final String PLUGIN_CHANGE_EVENT_MANAGER = "manager";

    private static final String INCLUDES = "includes";

    private static final String EXCLUDES = "excludes";

    private GrailsPluginClass pluginGrailsClass;

    private GroovyObject plugin;

    protected BeanWrapper pluginBean;

    private Closure onChangeListener;

    private Resource[] watchedResources = {};

    private PathMatchingResourcePatternResolver resolver;

    private String[] watchedResourcePatternReferences;

    private String[] loadAfterNames = {};

    private String[] loadBeforeNames = {};

    private String status = STATUS_ENABLED;

    private String[] observedPlugins;

    private Closure onConfigChangeListener;

    private Closure onShutdownListener;

    private Class<?>[] providedArtefacts = {};

    private Collection profiles = null;

    private Map pluginEnvs;

    private List<String> pluginExcludes = new ArrayList<>();

    private Collection<? extends TypeFilter> typeFilters = new ArrayList<>();

    private Resource pluginDescriptor;

    private List<WatchPattern> watchedResourcePatterns;

    public DefaultGrailsPlugin(Class<?> pluginClass, Resource resource, GrailsApplication application) {
        super(pluginClass, application);
        // create properties
        this.dependencies = Collections.emptyMap();
        this.pluginDescriptor = resource;
        this.resolver = CachingPathMatchingResourcePatternResolver.INSTANCE;

        try {
            initialisePlugin(pluginClass);
        }
        catch (Throwable e) {
            throw new PluginException("Error initialising plugin for class [" + pluginClass.getName() + "]:" + e.getMessage(), e);
        }
    }

    @Override
    public boolean isEnabled(String[] activeProfiles) {
        if (this.profiles == null) {
            return true;
        }
        else {
            for (String activeProfile : activeProfiles) {
                if (this.profiles.contains(activeProfile)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        if (this.plugin instanceof ApplicationContextAware) {
            ((ApplicationContextAware) this.plugin).setApplicationContext(applicationContext);
        }
        if (this.plugin instanceof ApplicationListener) {
            ((ConfigurableApplicationContext) applicationContext).addApplicationListener((ApplicationListener) this.plugin);
        }
    }

    @Override
    public List<WatchPattern> getWatchedResourcePatterns() {
        return this.watchedResourcePatterns;
    }

    @Override
    public boolean hasInterestInChange(String path) {
        if (this.watchedResourcePatterns != null) {
            for (WatchPattern watchedResourcePattern : this.watchedResourcePatterns) {
                if (watchedResourcePattern.matchesPath(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setManager(GrailsPluginManager manager) {
        super.setManager(manager);
        if (this.plugin instanceof Plugin) {
            ((Plugin) this.plugin).setPluginManager(manager);
        }
    }

    private void initialisePlugin(Class<?> clazz) {
        this.pluginGrailsClass = new GrailsPluginClass(clazz);
        this.plugin = (GroovyObject) this.pluginGrailsClass.newInstance();
        if (this.plugin instanceof Plugin) {
            Plugin p = (Plugin) this.plugin;
            p.setApplicationContext(this.applicationContext);
            p.setPlugin(this);
            p.setGrailsApplication(this.grailsApplication);
            p.setPluginManager(this.manager);
        }
        else if (this.plugin instanceof GrailsApplicationAware) {
            ((GrailsApplicationAware) this.plugin).setGrailsApplication(this.grailsApplication);
        }
        this.pluginBean = new BeanWrapperImpl(this.plugin);

        // configure plugin
        evaluatePluginVersion();
        evaluatePluginDependencies();
        evaluatePluginLoadAfters();
        evaluateProvidedArtefacts();
        evaluatePluginEvictionPolicy();
        evaluateOnChangeListener();
        evaluateObservedPlugins();
        evaluatePluginStatus();
        evaluatePluginScopes();
        evaluatePluginExcludes();
        evaluateTypeFilters();
    }

    @SuppressWarnings("unchecked")
    private void evaluateTypeFilters() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, TYPE_FILTERS);
        if (result instanceof List) {
            this.typeFilters = (List<TypeFilter>) result;
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginExcludes() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, PLUGIN_EXCLUDES);
        if (result instanceof List) {
            this.pluginExcludes = (List<String>) result;
        }
    }

    private void evaluatePluginScopes() {
        // Damn I wish Java had closures
        this.pluginEnvs = evaluateIncludeExcludeProperty(ENVIRONMENTS, new Closure(this) {
            private static final long serialVersionUID = 1;

            @Override
            public Object call(Object arguments) {
                String envName = (String) arguments;
                Environment env = Environment.getEnvironment(envName);
                if (env != null) {
                    return env.getName();
                }
                return arguments;
            }
        });
    }

    private Map evaluateIncludeExcludeProperty(String name, Closure converter) {
        Map resultMap = new HashMap();
        Object propertyValue = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, name);
        if (propertyValue instanceof Map) {
            Map containedMap = (Map) propertyValue;

            Object includes = containedMap.get(INCLUDES);
            evaluateAndAddIncludeExcludeObject(resultMap, includes, true, converter);

            Object excludes = containedMap.get(EXCLUDES);
            evaluateAndAddIncludeExcludeObject(resultMap, excludes, false, converter);
        }
        else {
            evaluateAndAddIncludeExcludeObject(resultMap, propertyValue, true, converter);
        }
        return resultMap;
    }

    private void evaluateAndAddIncludeExcludeObject(Map targetMap, Object includeExcludeObject, boolean include, Closure converter) {
        if (includeExcludeObject instanceof String) {
            final String includeExcludeString = (String) includeExcludeObject;
            evaluateAndAddToIncludeExcludeSet(targetMap, includeExcludeString, include, converter);
        }
        else if (includeExcludeObject instanceof List) {
            List includeExcludeList = (List) includeExcludeObject;
            evaluateAndAddListOfValues(targetMap, includeExcludeList, include, converter);
        }
    }

    private void evaluateAndAddListOfValues(Map targetMap, List includeExcludeList, boolean include, Closure converter) {
        for (Object value : includeExcludeList) {
            if (value instanceof String) {
                final String scopeName = (String) value;
                evaluateAndAddToIncludeExcludeSet(targetMap, scopeName, include, converter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluateAndAddToIncludeExcludeSet(Map targetMap, String includeExcludeString, boolean include, Closure converter) {
        Set set = lazilyCreateIncludeOrExcludeSet(targetMap, include);
        set.add(converter.call(includeExcludeString));
    }

    @SuppressWarnings("unchecked")
    private Set lazilyCreateIncludeOrExcludeSet(Map targetMap, boolean include) {
        String key = include ? INCLUDES : EXCLUDES;
        Set set = (Set) targetMap.get(key);
        if (set == null) {
            set = new HashSet();
            targetMap.put(key, set);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private void evaluateProvidedArtefacts() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, this.plugin, PROVIDED_ARTEFACTS);
        if (result instanceof Collection) {
            final Collection artefactList = (Collection) result;
            this.providedArtefacts = (Class<?>[]) artefactList.toArray(new Class[0]);
        }
    }

    private void evaluateProfiles() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, this.plugin, PROFILES);
        if (result instanceof Collection) {
            this.profiles = (Collection) result;
        }
    }

    public DefaultGrailsPlugin(Class<?> pluginClass, GrailsApplication application) {
        this(pluginClass, null, application);
    }

    private void evaluateObservedPlugins() {
        if (this.pluginBean.isReadableProperty(OBSERVE)) {
            Object observeProperty = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, this.plugin, OBSERVE);
            if (observeProperty instanceof Collection) {
                Collection observeList = (Collection) observeProperty;
                this.observedPlugins = new String[observeList.size()];
                int j = 0;
                for (Object anObserveList : observeList) {
                    String pluginName = anObserveList.toString();
                    this.observedPlugins[j++] = pluginName;
                }
            }
        }
        if (this.observedPlugins == null) {
            this.observedPlugins = new String[0];
        }
    }

    private void evaluatePluginStatus() {
        if (!this.pluginBean.isReadableProperty(STATUS)) {
            return;
        }

        Object statusObj = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, STATUS);
        if (statusObj != null) {
            this.status = statusObj.toString().toLowerCase();
        }
    }

    private void evaluateOnChangeListener() {
        if (this.pluginBean.isReadableProperty(ON_SHUTDOWN)) {
            this.onShutdownListener = (Closure) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, ON_SHUTDOWN);
        }
        if (this.pluginBean.isReadableProperty(ON_CONFIG_CHANGE)) {
            this.onConfigChangeListener = (Closure) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, ON_CONFIG_CHANGE);
        }
        if (this.pluginBean.isReadableProperty(ON_CHANGE)) {
            this.onChangeListener = (Closure) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, ON_CHANGE);
        }

        Environment env = Environment.getCurrent();
        final boolean warDeployed = env.isWarDeployed();
        final boolean reloadEnabled = env.isReloadEnabled();

        if (!((reloadEnabled || !warDeployed))) {
            return;
        }

        Object referencedResources = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, WATCHED_RESOURCES);

        try {
            List resourceList = null;
            if (referencedResources instanceof String) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Configuring plugin " + this + " to watch resources with pattern: " + referencedResources);
                }
                resourceList = Collections.singletonList(referencedResources.toString());
            }
            else if (referencedResources instanceof List) {
                resourceList = (List) referencedResources;
            }

            if (resourceList == null) {
                return;
            }

            List<String> resourceListTmp = new ArrayList<>();
            final String baseLocation = env.getReloadLocation();

            for (Object ref : resourceList) {
                String stringRef = ref.toString();
                if (warDeployed) {
                    addBaseLocationPattern(resourceListTmp, baseLocation, stringRef);
                }
                else {
                    addBaseLocationPattern(resourceListTmp, baseLocation, stringRef);
                }
            }

            this.watchedResourcePatternReferences = new String[resourceListTmp.size()];
            for (int i = 0; i < this.watchedResourcePatternReferences.length; i++) {
                String resRef = resourceListTmp.get(i);
                this.watchedResourcePatternReferences[i] = resRef;
            }

            this.watchedResourcePatterns = new WatchPatternParser().getWatchPatterns(Arrays.asList(this.watchedResourcePatternReferences));
        }
        catch (IllegalArgumentException e) {
            if (GrailsUtil.isDevelopmentEnv()) {
                logger.debug("Cannot load plug-in resource watch list from [" + GrailsArrayUtils.toString(this.watchedResourcePatternReferences) +
                        "]. This means that the plugin " + this +
                        ", will not be able to auto-reload changes effectively. Try running grails upgrade.: " + e.getMessage());
            }
        }

    }

    private void addBaseLocationPattern(List<String> resourceList, final String baseLocation, String pattern) {
        resourceList.add(baseLocation == null ? pattern : getResourcePatternForBaseLocation(baseLocation, pattern));
    }

    private String getResourcePatternForBaseLocation(String baseLocation, String resourcePath) {
        String location = baseLocation;
        if (!location.endsWith(File.separator)) {
            location = location + File.separator;
        }
        if (resourcePath.startsWith("./")) {
            return "file:" + location + resourcePath.substring(2);
        }
        else if (resourcePath.startsWith("file:./")) {
            return "file:" + location + resourcePath.substring(7);
        }
        return resourcePath;
    }

    private void evaluatePluginVersion() {
        if (!this.pluginBean.isReadableProperty(VERSION)) {
            throw new PluginException("Plugin [" + getName() + "] must specify a version!");
        }

        Object vobj = this.plugin.getProperty(VERSION);
        if (vobj == null) {
            throw new PluginException("Plugin " + this + " must specify a version. eg: def version = 0.1");
        }

        this.version = vobj.toString();
    }

    private void evaluatePluginEvictionPolicy() {
        if (!this.pluginBean.isReadableProperty(EVICT)) {
            return;
        }

        List pluginsToEvict = (List) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, this.plugin, EVICT);
        if (pluginsToEvict == null) {
            return;
        }

        this.evictionList = new String[pluginsToEvict.size()];
        int index = 0;
        for (Object o : pluginsToEvict) {
            this.evictionList[index++] = o == null ? "" : o.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginLoadAfters() {
        if (this.pluginBean.isReadableProperty(PLUGIN_LOAD_AFTER_NAMES)) {
            List loadAfterNamesList = (List) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(
                    this.pluginBean, this.plugin, PLUGIN_LOAD_AFTER_NAMES);
            if (loadAfterNamesList != null) {
                this.loadAfterNames = (String[]) loadAfterNamesList.toArray(new String[0]);
            }
        }
        if (this.pluginBean.isReadableProperty(PLUGIN_LOAD_BEFORE_NAMES)) {
            List loadBeforeNamesList = (List) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(
                    this.pluginBean, this.plugin, PLUGIN_LOAD_BEFORE_NAMES);
            if (loadBeforeNamesList != null) {
                this.loadBeforeNames = (String[]) loadBeforeNamesList.toArray(new String[0]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginDependencies() {
        if (!this.pluginBean.isReadableProperty(DEPENDS_ON)) {
            return;
        }

        this.dependencies = (Map) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, this.plugin, DEPENDS_ON);
        this.dependencyNames = this.dependencies.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getLoadAfterNames() {
        return this.loadAfterNames;
    }

    @Override
    public String[] getLoadBeforeNames() {
        return this.loadBeforeNames;
    }

    /**
     * @return the resolver
     */
    public PathMatchingResourcePatternResolver getResolver() {
        return this.resolver;
    }

    public ApplicationContext getParentCtx() {
        return this.grailsApplication.getParentContext();
    }

    public BeanBuilder beans(Closure closure) {
        BeanBuilder bb = new BeanBuilder(getParentCtx(), new GroovyClassLoader(this.grailsApplication.getClassLoader()));
        bb.invokeMethod("beans", new Object[] { closure });
        return bb;
    }

    public void doWithApplicationContext(ApplicationContext ctx) {
        if (this.plugin instanceof Plugin) {
            Plugin pluginObject = (Plugin) this.plugin;

            pluginObject.setApplicationContext(ctx);
            pluginObject.doWithApplicationContext();
        }
        else {
            Object[] args = { ctx };
            invokePluginHook(DO_WITH_APPLICATION_CONTEXT, args, ctx);
        }
    }

    private void invokePluginHook(String methodName, Object[] args, ApplicationContext ctx) {
        if (this.pluginBean.isReadableProperty(methodName)) {
            Closure c = (Closure) this.plugin.getProperty(methodName);
            c.setDelegate(this);
            c.call(args);
        }
        else {
            MetaClass pluginMetaClass = this.pluginGrailsClass.getMetaClass();
            if (!pluginMetaClass.respondsTo(this.plugin, methodName, args).isEmpty()) {
                pluginMetaClass.invokeMethod(this.plugin, methodName, ctx);
            }
        }
    }

    public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
        Binding b = new Binding();
        b.setVariable("application", this.grailsApplication);
        b.setVariable(GrailsApplication.APPLICATION_ID, this.grailsApplication);
        b.setVariable("manager", getManager());
        b.setVariable("plugin", this);
        b.setVariable("parentCtx", getParentCtx());
        b.setVariable("resolver", getResolver());

        if (this.plugin instanceof Plugin) {
            Closure c = ((Plugin) this.plugin).doWithSpring();
            if (c != null) {
                BeanBuilder bb = new BeanBuilder(getParentCtx(), springConfig, this.grailsApplication.getClassLoader());
                bb.setBeanBuildResource(new DescriptiveResource(this.plugin.getClass().getName()));
                bb.setBinding(b);
                bb.invokeMethod("beans", new Object[] { c });
            }
        }
        else {

            if (!this.pluginBean.isReadableProperty(DO_WITH_SPRING)) {
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Plugin " + this + " is participating in Spring configuration...");
            }

            Closure c = (Closure) this.plugin.getProperty(DO_WITH_SPRING);
            BeanBuilder bb = new BeanBuilder(getParentCtx(), springConfig, this.grailsApplication.getClassLoader());
            bb.setBeanBuildResource(new DescriptiveResource(this.plugin.getClass().getName()));
            bb.setBinding(b);
            c.setDelegate(bb);
            bb.invokeMethod("beans", new Object[] { c });
        }

    }

    @Override
    public String getName() {
        return this.pluginGrailsClass.getLogicalPropertyName();
    }

    @SuppressWarnings("unchecked")
    private void addExcludeRuleInternal(Map map, Object o) {
        Collection excludes = (Collection) map.get(EXCLUDES);
        if (excludes == null) {
            excludes = new ArrayList();
            map.put(EXCLUDES, excludes);
        }
        Collection includes = (Collection) map.get(INCLUDES);
        if (includes != null) {
            includes.remove(o);
        }
        excludes.add(o);
    }

    public void addExclude(Environment env) {
        addExcludeRuleInternal(this.pluginEnvs, env);
    }

    public boolean supportsEnvironment(Environment environment) {
        return supportsValueInIncludeExcludeMap(this.pluginEnvs, environment.getName());
    }

    public boolean supportsCurrentScopeAndEnvironment() {
        Environment e = Environment.getCurrent();
        return supportsEnvironment(e);
    }

    private boolean supportsValueInIncludeExcludeMap(Map includeExcludeMap, Object value) {
        if (includeExcludeMap.isEmpty()) {
            return true;
        }

        Set includes = (Set) includeExcludeMap.get(INCLUDES);
        if (includes != null) {
            return includes.contains(value);
        }

        Set excludes = (Set) includeExcludeMap.get(EXCLUDES);
        return !(excludes != null && excludes.contains(value));
    }

    /**
     * @deprecated Dynamic document generation no longer supported
     * @param text
     */
    @Deprecated
    public void doc(String text) {
        // no-op
    }

    @Override
    public String[] getDependencyNames() {
        return this.dependencyNames;
    }

    /**
     * @return the watchedResources
     */
    public Resource[] getWatchedResources() {
        if (this.watchedResourcePatternReferences != null && this.watchedResources.length == 0) {
            for (String resourcesReference : this.watchedResourcePatternReferences) {
                try {
                    Resource[] resources = this.resolver.getResources(resourcesReference);
                    if (resources.length > 0) {
                        this.watchedResources = (Resource[]) GrailsArrayUtils.addAll(this.watchedResources, resources);
                    }
                }
                catch (Exception ignored) {
                }
            }
        }
        return this.watchedResources;
    }

    @Override
    public String getDependentVersion(String name) {
        Object dependentVersion = this.dependencies.get(name);
        if (dependentVersion == null) {
            throw new PluginException("Plugin [" + getName() + "] referenced dependency [" + name + "] with no version!");
        }
        return dependentVersion.toString();
    }

    @Override
    public String toString() {
        return "[" + getName() + ":" + getVersion() + "]";
    }

    public void setWatchedResources(Resource[] watchedResources) throws IOException {
        this.watchedResources = watchedResources;
    }

    /*
     * These two properties help the closures to resolve a log and plugin variable during executing
     */
    public Log getLog() {
        return logger;
    }

    public GrailsPlugin getPlugin() {
        return this;
    }

    public void setParentApplicationContext(ApplicationContext parent) {
        // do nothing for the moment
    }

    /* (non-Javadoc)
     * @see org.grails.plugins.AbstractGrailsPlugin#refresh()
     */
    @Override
    public void refresh() {
        // do nothing
        org.grails.io.support.Resource descriptor = getDescriptor();
        if (this.grailsApplication == null || descriptor == null) {
            return;
        }

        ClassLoader parent = this.grailsApplication.getClassLoader();
        GroovyClassLoader gcl = new GroovyClassLoader(parent);
        try {
            initialisePlugin(gcl.parseClass(descriptor.getFile()));
        }
        catch (Exception e) {
            logger.error("Error refreshing plugin: " + e.getMessage(), e);
        }
    }

    public GroovyObject getInstance() {
        return this.plugin;
    }

    public void doWithDynamicMethods(ApplicationContext ctx) {
        if (this.plugin instanceof Plugin) {
            ((Plugin) this.plugin).doWithDynamicMethods();
        }
        else {
            Object[] args = { ctx };
            invokePluginHook(DO_WITH_DYNAMIC_METHODS, args, ctx);
        }
    }

    public boolean isEnabled() {
        if (this.plugin instanceof Plugin) {
            return ((Plugin) this.plugin).isEnabled();
        }
        else {
            return STATUS_ENABLED.equals(this.status);
        }
    }

    public String[] getObservedPluginNames() {
        return this.observedPlugins;
    }

    public void notifyOfEvent(Map event) {
        if (this.plugin instanceof Plugin) {
            ((Plugin) this.plugin).onChange(event);
        }
        else if (this.onChangeListener != null) {
            invokeOnChangeListener(event);
        }
    }

    public Map notifyOfEvent(int eventKind, final Object source) {
        @SuppressWarnings("unchecked")
        Map<String, Object> event = CollectionUtils.<String, Object>newMap(
                PLUGIN_CHANGE_EVENT_SOURCE, source,
                PLUGIN_CHANGE_EVENT_PLUGIN, this.plugin,
                PLUGIN_CHANGE_EVENT_APPLICATION, this.grailsApplication,
                PLUGIN_CHANGE_EVENT_MANAGER, getManager(),
                PLUGIN_CHANGE_EVENT_CTX, this.applicationContext);

        switch (eventKind) {
            case EVENT_ON_CHANGE:
                if (this.plugin instanceof Plugin) {
                    ((Plugin) this.plugin).onChange(event);
                }
                else {
                    notifyOfEvent(event);
                }
                getManager().informObservers(getName(), event);
                break;
            case EVENT_ON_SHUTDOWN:
                if (this.plugin instanceof Plugin) {
                    ((Plugin) this.plugin).onShutdown(event);
                }
                else {
                    invokeOnShutdownEventListener(event);
                }
                break;

            case EVENT_ON_CONFIG_CHANGE:
                if (this.plugin instanceof Plugin) {
                    ((Plugin) this.plugin).onConfigChange(event);
                }
                else {

                    invokeOnConfigChangeListener(event);
                }
                break;
            default:
                notifyOfEvent(event);
        }

        return event;
    }

    private void invokeOnShutdownEventListener(Map event) {
        callEvent(this.onShutdownListener, event);
    }

    private void invokeOnConfigChangeListener(Map event) {
        callEvent(this.onConfigChangeListener, event);
    }

    private void callEvent(Closure closureHook, Map event) {
        if (closureHook == null) {
            return;
        }

        closureHook.setDelegate(this);
        closureHook.call(new Object[] { event });
    }

    private void invokeOnChangeListener(Map event) {
        this.onChangeListener.setDelegate(this);
        this.onChangeListener.call(new Object[] { event });

        if (!(this.applicationContext instanceof GenericApplicationContext)) {
            return;
        }

        // Apply any factory post processors in case the change listener has changed any
        // bean definitions (GRAILS-5763)
        GenericApplicationContext ctx = (GenericApplicationContext) this.applicationContext;
        ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
        for (BeanFactoryPostProcessor postProcessor : ctx.getBeanFactoryPostProcessors()) {
            try {
                postProcessor.postProcessBeanFactory(beanFactory);
            }
            catch (IllegalStateException ignored) {
                // post processor doesn't allow running again, just continue
            }
        }
    }

    public void doArtefactConfiguration() {
        if (!this.pluginBean.isReadableProperty(ARTEFACTS)) {
            return;
        }

        List l;
        if (this.plugin instanceof Plugin) {
            l = ((Plugin) this.plugin).getArtefacts();
        }
        else {

            l = (List) this.plugin.getProperty(ARTEFACTS);
        }
        for (Object artefact : l) {
            if (artefact instanceof Class) {
                Class artefactClass = (Class) artefact;
                if (ArtefactHandler.class.isAssignableFrom(artefactClass)) {
                    try {
                        this.grailsApplication.registerArtefactHandler((ArtefactHandler) artefactClass.newInstance());
                    }
                    catch (InstantiationException e) {
                        logger.error("Cannot instantiate an Artefact Handler:" + e.getMessage(), e);
                    }
                    catch (IllegalAccessException e) {
                        logger.error("The constructor of the Artefact Handler is not accessible:" + e.getMessage(), e);
                    }
                }
                else {
                    logger.error("This class is not an ArtefactHandler:" + artefactClass.getName());
                }
            }
            else if (artefact instanceof ArtefactHandler) {
                this.grailsApplication.registerArtefactHandler((ArtefactHandler) artefact);
            }
            else {
                logger.error("This object is not an ArtefactHandler:" + artefact + "[" + artefact.getClass().getName() + "]");
            }
        }
    }

    public Class<?>[] getProvidedArtefacts() {
        return this.providedArtefacts;
    }

    public List<String> getPluginExcludes() {
        return this.pluginExcludes;
    }

    public Collection<? extends TypeFilter> getTypeFilters() {
        return this.typeFilters;
    }

    public String getFullName() {
        return getName() + '-' + getVersion();
    }

    public org.grails.io.support.Resource getDescriptor() {
        return new SpringResource(this.pluginDescriptor);
    }

    public void setDescriptor(Resource descriptor) {
        this.pluginDescriptor = descriptor;
    }

    public org.grails.io.support.Resource getPluginDir() {
        try {
            return new SpringResource(this.pluginDescriptor.createRelative("."));
        }
        catch (IOException ignored) {
            return null;
        }
    }

    public Map getProperties() {
        return DefaultGroovyMethods.getProperties(this.plugin);
    }

}
