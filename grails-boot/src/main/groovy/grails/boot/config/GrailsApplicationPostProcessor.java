/*
 * Copyright 2014-2022 the original author or authors.
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
package grails.boot.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import groovy.lang.Binding;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.Assert;

import grails.artefact.Artefact;
import grails.config.Settings;
import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import grails.core.GrailsApplicationLifeCycle;
import grails.plugins.DefaultGrailsPluginManager;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.spring.BeanBuilder;
import grails.util.CollectionUtils;
import grails.util.Environment;
import grails.util.Holders;

import org.grails.config.NavigableMap;
import org.grails.config.PrefixedMapPropertySource;
import org.grails.config.PropertySourcesConfig;
import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.core.support.GrailsApplicationAwareBeanPostProcessor;
import org.grails.plugins.GrailsPluginArtefactHandler;
import org.grails.plugins.support.PluginManagerAwareBeanPostProcessor;
import org.grails.spring.DefaultRuntimeSpringConfiguration;
import org.grails.spring.RuntimeSpringConfigUtilities;
import org.grails.spring.RuntimeSpringConfiguration;

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that enhances any ApplicationContext with plugin manager capabilities
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 * @see GrailsApplicationEventListener
 */
public class GrailsApplicationPostProcessor
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, ApplicationStartupAware, Ordered {

    public static final String BEAN_NAME = "grailsApplicationPostProcessor";

    private static final boolean RELOADING_ENABLED = Environment.isReloadingAgentEnabled();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final GrailsApplication grailsApplication;

    protected final GrailsPluginManager pluginManager;

    protected final GrailsApplicationEventListener grailsApplicationEventListener;

    protected ApplicationContext applicationContext;

    protected GrailsApplicationLifeCycle lifeCycle;

    protected Set<Class<?>> classes;

    protected boolean loadExternalBeans = true;

    protected boolean reloadingEnabled = RELOADING_ENABLED;

    /** Application startup metrics. **/
    private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

    public GrailsApplicationPostProcessor() {
        this.grailsApplication = new DefaultGrailsApplication();
        this.grailsApplicationEventListener = new GrailsApplicationEventListener();
        this.pluginManager = new DefaultGrailsPluginManager(this.grailsApplication);
    }

    public void setGrailsApplicationLifeCycle(GrailsApplicationLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public void setLoadExternalBeans(boolean loadExternalBeans) {
        this.loadExternalBeans = loadExternalBeans;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (this.applicationContext != applicationContext) {
            this.applicationContext = applicationContext;
            initializeGrailsApplication(applicationContext);
            if (applicationContext instanceof ConfigurableApplicationContext) {
                ConfigurableApplicationContext configurable = (ConfigurableApplicationContext) applicationContext;
                configurable.addApplicationListener(this.grailsApplicationEventListener);
                configurable.getEnvironment().addActiveProfile(
                        this.grailsApplication.getConfig().getProperty(Settings.PROFILE, String.class, "web"));
            }
        }
    }

    protected final void initializeGrailsApplication(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext should not be null");
        }
        Environment.setInitializing(true);
        this.grailsApplication.setApplicationContext(applicationContext);
        this.grailsApplication.setMainContext(applicationContext);
        this.classes = loadArtefactClasses();
        this.classes.stream().filter(GrailsPluginArtefactHandler::isGrailsPlugin)
                .forEach(this.pluginManager::addUserPlugin);
        customizePluginManager(this.pluginManager);
        this.pluginManager.loadPlugins();
        this.pluginManager.setApplicationContext(applicationContext);
        loadApplicationConfig();
        customizeGrailsApplication(this.grailsApplication);
        performGrailsInitializationSequence();
    }

    protected void customizePluginManager(GrailsPluginManager grailsApplication) {
    }

    protected void customizeGrailsApplication(GrailsApplication grailsApplication) {
    }

    protected void performGrailsInitializationSequence() {
        this.pluginManager.doArtefactConfiguration();
        this.grailsApplication.initialise();
        // register plugin provided classes first, this gives the opportunity
        // for application classes to override those provided by a plugin
        this.pluginManager.registerProvidedArtefacts(this.grailsApplication);
        this.pluginManager.registerProvidedModules();

        this.classes.forEach(this.grailsApplication::addArtefact);
    }

    protected Set<Class<?>> loadArtefactClasses() {
        StartupStep artefactStep = this.applicationStartup.start("grails.application.artefact-classes.loaded");
        GrailsComponentScanner scanner = new GrailsComponentScanner(this.applicationContext, this.applicationStartup);
        Set<Class<?>> classes;
        try {
            classes = scanner.scan(Artefact.class);
        }
        catch (ClassNotFoundException ignored) {
            classes = Collections.emptySet();
        }

        artefactStep.tag("classCount", String.valueOf(classes.size())).end();
        return classes;
    }

    protected void loadApplicationConfig() {
        StartupStep configStep = this.applicationStartup.start("grails.application.config.prepared");
        org.springframework.core.env.Environment environment = this.applicationContext.getEnvironment();
        ConfigurableConversionService conversionService = null;
        if (environment instanceof ConfigurableEnvironment) {
            if (environment instanceof AbstractEnvironment) {
                conversionService = ((AbstractEnvironment) environment).getConversionService();
                conversionService.addConverter(new Converter<String, Resource>() {

                    @Override
                    public Resource convert(String source) {
                        return GrailsApplicationPostProcessor.this.applicationContext.getResource(source);
                    }

                });
                conversionService.addConverter(new Converter<NavigableMap.NullSafeNavigator, String>() {

                    @Override
                    public String convert(NavigableMap.NullSafeNavigator source) {
                        return null;
                    }

                });
                conversionService.addConverter(new Converter<NavigableMap.NullSafeNavigator, Object>() {

                    @Override
                    public Object convert(NavigableMap.NullSafeNavigator source) {
                        return null;
                    }

                });
            }

            MutablePropertySources propertySources = ((ConfigurableEnvironment) environment).getPropertySources();
            GrailsPlugin[] plugins = this.pluginManager.getAllPlugins();
            List<GrailsPlugin> pluginList = Arrays.asList(plugins);
            Collections.reverse(pluginList);

            if (pluginList.size() > 0) {
                for (GrailsPlugin plugin : pluginList) {
                    PropertySource<?> pluginPropertySource = plugin.getPropertySource();
                    if (pluginPropertySource != null) {
                        if (pluginPropertySource instanceof EnumerablePropertySource) {
                            propertySources.addLast(new PrefixedMapPropertySource("grails.plugins.$plugin.name",
                                    (EnumerablePropertySource) pluginPropertySource));
                        }
                        propertySources.addLast(pluginPropertySource);
                    }
                }
            }

            PropertySourcesConfig config = new PropertySourcesConfig(propertySources);
            if (conversionService != null) {
                config.setConversionService(conversionService);
            }
            ((DefaultGrailsApplication) this.grailsApplication).setConfig(config);
        }
        configStep.end();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        StartupStep springConfigStep = this.applicationStartup.start("grails.application.bean-definitions.registered");
        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();

        GrailsApplication application = this.grailsApplication;
        Holders.setGrailsApplication(application);

        // first register plugin beans
        this.pluginManager.doRuntimeConfiguration(springConfig);

        if (this.loadExternalBeans) {
            // now allow overriding via application
            ApplicationContext context = application.getMainContext();
            Resource beanResources = context.getResource(RuntimeSpringConfigUtilities.SPRING_RESOURCES_GROOVY);
            if (beanResources.exists()) {
                try {
                    Map<String, Object> variables = CollectionUtils.newMap(
                            "application", application,
                            "grailsApplication", application);
                    RuntimeSpringConfigUtilities.reloadSpringResourcesConfig(springConfig, variables, beanResources);
                }
                catch (Throwable e) {
                    this.log.error("Error loading spring/resources.groovy file: ${e.message}", e);
                    throw new GrailsConfigurationException("Error loading spring/resources.groovy file: ${e.message}", e);
                }
            }

            beanResources = context.getResource(RuntimeSpringConfigUtilities.SPRING_RESOURCES_XML);
            if (beanResources.exists()) {
                try {
                    new BeanBuilder(null, springConfig, application.getClassLoader())
                            .importBeans(beanResources);
                }
                catch (Throwable e) {
                    this.log.error("Error loading spring/resources.xml file: ${e.message}", e);
                    throw new GrailsConfigurationException("Error loading spring/resources.xml file: ${e.message}", e);
                }
            }
        }

        Binding b = new Binding();
        b.setVariable(GrailsApplication.APPLICATION_ID, this.grailsApplication);
        b.setVariable("application", this.grailsApplication);
        b.setVariable("manager", this.pluginManager);
        if (this.lifeCycle != null) {
            Closure<?> withSpring = this.lifeCycle.doWithSpring();
            if (withSpring != null) {
                BeanBuilder bb = new BeanBuilder(null, springConfig, application.getClassLoader());
                bb.setBeanBuildResource(new DescriptiveResource(this.lifeCycle.getClass().getName()));
                bb.setBinding(b);
                bb.beans(withSpring);
            }
        }

        Collection<GrailsApplicationLifeCycle> lifeCycles = this.applicationContext.getBeansOfType(GrailsApplicationLifeCycle.class)
                .values()
                .stream()
                .sorted(OrderComparator.INSTANCE)
                .collect(Collectors.toList());
        for (GrailsApplicationLifeCycle lifeCycle : lifeCycles) {
            Closure<?> withSpring = lifeCycle.doWithSpring();
            if (withSpring != null) {
                BeanBuilder bb = new BeanBuilder(null, springConfig, application.getClassLoader());
                bb.setBeanBuildResource(new DescriptiveResource(lifeCycle.getClass().getName()));
                bb.setBinding(b);
                bb.beans(withSpring);
            }
        }

        springConfig.registerBeansWithRegistry(registry);
        springConfigStep.tag("beanDefinitionCount", String.valueOf(springConfig.getBeanNames().size())).end();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
        if (parentBeanFactory instanceof ConfigurableBeanFactory) {
            ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) parentBeanFactory;
            configurableBeanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, this.grailsApplication);
            configurableBeanFactory.registerSingleton(GrailsPluginManager.BEAN_NAME, this.pluginManager);
        }
        else {
            beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, this.grailsApplication);
            beanFactory.registerSingleton(GrailsPluginManager.BEAN_NAME, this.pluginManager);
        }

        beanFactory.addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(this.grailsApplication));
        beanFactory.addBeanPostProcessor(new PluginManagerAwareBeanPostProcessor(this.pluginManager));
    }

    @Override
    public void setApplicationStartup(ApplicationStartup applicationStartup) {
        Assert.notNull(applicationStartup, "applicationStartup should not be null");
        this.applicationStartup = applicationStartup;
        this.pluginManager.setApplicationStartup(applicationStartup);
        this.grailsApplicationEventListener.setApplicationStartup(applicationStartup);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
