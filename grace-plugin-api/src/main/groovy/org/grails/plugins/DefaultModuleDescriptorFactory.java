/*
 * Copyright 2021-2024 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import grails.plugins.ModuleDescriptor;
import grails.plugins.ModuleDescriptorFactory;
import grails.plugins.exceptions.PluginException;

/**
 * Default implementation of {@link ModuleDescriptorFactory}
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory {

    private final Map<String, Class<? extends ModuleDescriptor>> moduleDescriptorClasses = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    protected static final Log logger = LogFactory.getLog(DefaultModuleDescriptorFactory.class);

    public DefaultModuleDescriptorFactory() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <M> Class<? extends ModuleDescriptor<M>> getModuleDescriptorClass(String type) {
        return (Class<? extends ModuleDescriptor<M>>) this.moduleDescriptorClasses.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <M> ModuleDescriptor<M> getModuleDescriptor(String type) throws PluginException, ClassNotFoundException {
        Class<? extends ModuleDescriptor> moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null) {
            String message = "ModuleDescriptor class for module type '" + type + "' not found in dynamic plugins";
            logger.warn(message);
            return null;
        }

        return (ModuleDescriptor<M>) create(moduleDescriptorClazz);
    }

    @Override
    public boolean hasModuleDescriptor(String type) {
        return this.moduleDescriptorClasses.containsKey(type);
    }

    @Override
    public void addModuleDescriptor(String type, Class<? extends ModuleDescriptor> moduleDescriptorClass) {
        this.moduleDescriptorClasses.put(type, moduleDescriptorClass);
    }

    public void removeModuleDescriptorForType(String type) {
        this.moduleDescriptorClasses.remove(type);
    }

    protected Map<String, Class<? extends ModuleDescriptor>> getDescriptorClassesMap() {
        return Collections.unmodifiableMap(this.moduleDescriptorClasses);
    }

    public <T> T create(Class<T> moduleClass) throws IllegalArgumentException {
        AutowireCapableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
        Object object = beanFactory.createBean(moduleClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        return moduleClass.cast(object);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
