/*
 * Copyright 2021-2022 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import grails.plugins.ModuleDescriptor;
import grails.plugins.ModuleDescriptorFactory;
import grails.plugins.exceptions.PluginException;

public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory, ApplicationContextAware {

    private final Map<String, Class<? extends ModuleDescriptor>> moduleDescriptorClasses = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    public DefaultModuleDescriptorFactory() {
    }

    public Class<? extends ModuleDescriptor> getModuleDescriptorClass(String type) {
        return this.moduleDescriptorClasses.get(type);
    }

    public ModuleDescriptor<?> getModuleDescriptor(String type) throws PluginException, ClassNotFoundException {
        Class<? extends ModuleDescriptor> moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null) {
            throw new PluginException("Cannot find ModuleDescriptor class for plugin of type '" + type + "'.");
        }

        return create(moduleDescriptorClazz);
    }

    public boolean hasModuleDescriptor(String type) {
        return this.moduleDescriptorClasses.containsKey(type);
    }

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
        try {
            return moduleClass.newInstance();
        }
        catch (InstantiationException e) {
            throw new IllegalArgumentException("Unable to instantiate constructor", e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to access constructor", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
