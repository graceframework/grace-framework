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
package org.grails.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

/**
 * A programmable runtime Spring configuration that allows a spring ApplicationContext
 * to be constructed at runtime.
 *
 * Credit must go to Solomon Duskis and the
 * article: <a href="http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring">Programmatic Configuration in Spring</a>
 *
 * @author Graeme
 * @since 0.3
 */
public class DefaultRuntimeSpringConfiguration implements RuntimeSpringConfiguration {

    private static final Log logger = LogFactory.getLog(DefaultRuntimeSpringConfiguration.class);

    protected GenericApplicationContext context;

    private final Map<String, BeanConfiguration> beanConfigs = new HashMap<>();

    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    private final Set<String> beanNames = new LinkedHashSet<>();

    protected ApplicationContext parent;

    protected ClassLoader classLoader;

    protected final Map<String, List<String>> aliases = new HashMap<>();

    protected ListableBeanFactory beanFactory;

    /**
     * Creates the ApplicationContext instance. Subclasses can override to customise the used ApplicationContext
     *
     * @param parentCtx The parent ApplicationContext instance. Can be null.
     *
     * @return An instance of GenericApplicationContext
     */
    protected GenericApplicationContext createApplicationContext(ApplicationContext parentCtx) {
        if (parentCtx != null && this.beanFactory != null) {
            Assert.isInstanceOf(DefaultListableBeanFactory.class, this.beanFactory,
                    "ListableBeanFactory set must be a subclass of DefaultListableBeanFactory");

            return new GrailsApplicationContext((DefaultListableBeanFactory) this.beanFactory, parentCtx);
        }

        if (this.beanFactory != null) {
            Assert.isInstanceOf(DefaultListableBeanFactory.class, this.beanFactory,
                    "ListableBeanFactory set must be a subclass of DefaultListableBeanFactory");

            return new GrailsApplicationContext((DefaultListableBeanFactory) this.beanFactory);
        }

        if (parentCtx != null) {
            return new GrailsApplicationContext(parentCtx);
        }

        return new GrailsApplicationContext();
    }

    public DefaultRuntimeSpringConfiguration() {
        super();
    }

    public DefaultRuntimeSpringConfiguration(ApplicationContext parent) {
        this(parent, null);
    }

    public DefaultRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader cl) {
        this.parent = parent;
        this.classLoader = cl;
    }

    private void trySettingClassLoaderOnContextIfFoundInParent(ApplicationContext parentCtx) {
        if (parentCtx.containsBean("classLoader")) {
            Object cl = parentCtx.getBean("classLoader");
            if (cl instanceof ClassLoader) {
                setClassLoaderOnContext((ClassLoader) cl);
            }
        }
    }

    private void setClassLoaderOnContext(ClassLoader cl) {
        this.context.setClassLoader(cl);
        this.context.getBeanFactory().setBeanClassLoader(cl);
    }

    /**
     * Initialises the ApplicationContext instance.
     */
    protected void initialiseApplicationContext() {
        if (this.context != null) {
            return;
        }

        this.context = createApplicationContext(this.parent);

        if (this.parent != null && this.classLoader == null) {
            trySettingClassLoaderOnContextIfFoundInParent(this.parent);
        }
        else if (this.classLoader != null) {
            setClassLoaderOnContext(this.classLoader);
        }

        Assert.notNull(this.context, "ApplicationContext cannot be null");
    }

    public BeanConfiguration addSingletonBean(String name, @SuppressWarnings("rawtypes") Class clazz) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name, clazz);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration addPrototypeBean(String name, @SuppressWarnings("rawtypes") Class clazz) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name, clazz, true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public ApplicationContext getApplicationContext() {
        long now = logger.isDebugEnabled() ? System.currentTimeMillis() : 0;
        initialiseApplicationContext();
        registerBeansWithContext(this.context);
        this.context.refresh();
        if (logger.isDebugEnabled()) {
            logger.debug("Created ApplicationContext in " + (System.currentTimeMillis() - now) + "ms");
        }
        return this.context;
    }

    public ApplicationContext getUnrefreshedApplicationContext() {
        initialiseApplicationContext();
        return this.context;
    }

    public BeanConfiguration addSingletonBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration createSingletonBean(@SuppressWarnings("rawtypes") Class clazz) {
        return new DefaultBeanConfiguration(clazz);
    }

    @SuppressWarnings("rawtypes")
    public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name, clazz, args);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration addPrototypeBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name, true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    private void registerBeanConfiguration(String name, BeanConfiguration bc) {
        this.beanConfigs.put(name, bc);
        this.beanNames.add(name);
    }

    @SuppressWarnings("rawtypes")
    public BeanConfiguration createSingletonBean(Class clazz, Collection constructorArguments) {
        return new DefaultBeanConfiguration(clazz, constructorArguments);
    }

    public BeanConfiguration createPrototypeBean(String name) {
        return new DefaultBeanConfiguration(name, true);
    }

    public BeanConfiguration createSingletonBean(String name) {
        return new DefaultBeanConfiguration(name);
    }

    public void addBeanConfiguration(String beanName, BeanConfiguration beanConfiguration) {
        beanConfiguration.setName(beanName);
        registerBeanConfiguration(beanName, beanConfiguration);
    }

    public void addBeanDefinition(String name, BeanDefinition bd) {
        this.beanDefinitions.put(name, bd);
        this.beanConfigs.remove(name);
        this.beanNames.add(name);
    }

    public boolean containsBean(String name) {
        return this.beanNames.contains(name);
    }

    public BeanConfiguration getBeanConfig(String name) {
        return this.beanConfigs.get(name);
    }

    public AbstractBeanDefinition createBeanDefinition(String name) {
        if (containsBean(name)) {
            if (this.beanDefinitions.containsKey(name)) {
                return (AbstractBeanDefinition) this.beanDefinitions.get(name);
            }
            if (this.beanConfigs.containsKey(name)) {
                return this.beanConfigs.get(name).getBeanDefinition();
            }
        }
        return null;
    }

    public void registerPostProcessor(BeanFactoryPostProcessor processor) {
        initialiseApplicationContext();
        this.context.addBeanFactoryPostProcessor(processor);
    }

    public List<String> getBeanNames() {
        return Collections.unmodifiableList(new ArrayList<>(this.beanNames));
    }

    public void registerBeansWithContext(GenericApplicationContext applicationContext) {
        registerBeansWithRegistry(applicationContext);
    }

    public void registerBeansWithRegistry(BeanDefinitionRegistry registry) {
        registerUnrefreshedBeansWithRegistry(registry);
        registerBeanConfigsWithRegistry(registry);
        registerBeanDefinitionsWithRegistry(registry);
        registerBeanAliasesWithRegistry(registry);
    }

    private void registerUnrefreshedBeansWithRegistry(BeanDefinitionRegistry registry) {
        if (this.context != null) {
            for (String beanName : this.context.getBeanDefinitionNames()) {
                registry.registerBeanDefinition(beanName, this.context.getBeanDefinition(beanName));
            }
        }
    }

    private void registerBeanConfigsWithRegistry(BeanDefinitionRegistry registry) {
        for (BeanConfiguration bc : this.beanConfigs.values()) {
            String beanName = bc.getName();

            if (bc.isConditionOn()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Registering bean definition for bean name '" + beanName + "'");
                    if (logger.isTraceEnabled()) {
                        MutablePropertyValues pvs = bc.getBeanDefinition().getPropertyValues();
                        for (PropertyValue pv : pvs) {
                            logger.trace("    with property: " + pv.getName() + " = " + pv.getValue());
                        }
                    }
                }

                registry.registerBeanDefinition(beanName, bc.getBeanDefinition());
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean '" + beanName + "' is not registered with bean.condition = false");
                }
            }
        }
    }

    private void registerBeanDefinitionsWithRegistry(BeanDefinitionRegistry registry) {
        for (Object key : this.beanDefinitions.keySet()) {
            BeanDefinition bd = this.beanDefinitions.get(key);
            if (logger.isDebugEnabled()) {
                logger.debug("Registering bean definition for bean name '" + key + "'");
                if (logger.isTraceEnabled()) {
                    MutablePropertyValues pvs = bd.getPropertyValues();
                    for (PropertyValue pv : pvs) {
                        logger.trace("    with property: " + pv.getName() + " = " + pv.getValue());
                    }
                }
            }
            final String beanName = key.toString();
            registry.registerBeanDefinition(beanName, bd);
        }
    }

    public void registerBeansWithConfig(RuntimeSpringConfiguration targetSpringConfig) {
        if (targetSpringConfig == null) {
            return;
        }

        ApplicationContext ctx = targetSpringConfig.getUnrefreshedApplicationContext();
        if (ctx instanceof BeanDefinitionRegistry) {
            final BeanDefinitionRegistry registry = (BeanDefinitionRegistry) ctx;
            registerUnrefreshedBeansWithRegistry(registry);
            registerBeansWithRegistry(registry);
        }
        for (Map.Entry<String, BeanConfiguration> beanEntry : this.beanConfigs.entrySet()) {
            String beanName = beanEntry.getKey();
            BeanConfiguration bc = beanEntry.getValue();
            if (beanEntry.getValue().isConditionOn()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Registering bean definition for bean name '" + beanName + "'");
                    if (logger.isTraceEnabled()) {
                        MutablePropertyValues pvs = bc.getBeanDefinition().getPropertyValues();
                        for (PropertyValue pv : pvs) {
                            logger.trace("    with property: " + pv.getName() + " = " + pv.getValue());
                        }
                    }
                }

                targetSpringConfig.addBeanConfiguration(beanEntry.getKey(), beanEntry.getValue());
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean '" + beanName + "' is not registered with bean.condition = false");
                }
            }
        }
    }

    private void registerBeanAliasesWithRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
        for (Map.Entry<String, List<String>> entry : this.aliases.entrySet()) {
            String beanName = entry.getKey();
            List<String> beanAliases = entry.getValue();
            if (beanAliases != null && !beanAliases.isEmpty()) {
                for (String alias : beanAliases) {
                    beanDefinitionRegistry.registerAlias(beanName, alias);
                }
            }
        }
    }

    private void removeBeanDefinition(BeanDefinitionRegistry registry, String beanName) {
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(registry.getClass());
        if (!mc.respondsTo(registry, "removeBeanDefinition").isEmpty()) {
            mc.invokeMethod(registry, "removeBeanDefinition", new Object[] { beanName });
        }
    }

    public BeanConfiguration addAbstractBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name);
        bc.setAbstract(true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public void addAlias(String alias, String beanName) {
        List<String> beanAliases = this.aliases.computeIfAbsent(beanName, k -> new ArrayList<>());
        beanAliases.add(alias);
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefinitions.get(beanName);
    }

    public void setBeanFactory(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

}
