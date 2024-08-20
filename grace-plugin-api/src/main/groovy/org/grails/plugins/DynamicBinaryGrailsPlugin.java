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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;

import grails.core.GrailsApplication;
import grails.plugins.DynamicGrailsPlugin;
import grails.plugins.DynamicPlugin;
import grails.plugins.ModuleDescriptor;
import grails.plugins.ModuleDescriptorFactory;
import grails.util.GrailsClassUtils;

/**
 * Binary plugin with dynamic modules.
 *
 * @author Michael Yan
 * @see BinaryGrailsPlugin
 * @since 2022.0.0
 */
public class DynamicBinaryGrailsPlugin extends BinaryGrailsPlugin implements DynamicGrailsPlugin {

    private final Map<String, ModuleDescriptor<?>> modules = new LinkedHashMap<>();

    private ModuleDescriptorFactory moduleDescriptorFactory;

    private Object providedModules;

    /**
     * Creates a binary plugin instance.
     *
     * @param pluginClass The plugin class
     * @param descriptor  The META-INF/grails-plugin.xml descriptor
     * @param application The application
     */
    public DynamicBinaryGrailsPlugin(Class<?> pluginClass, BinaryGrailsPluginDescriptor descriptor, GrailsApplication application) {
        super(pluginClass, descriptor, application);

        evaluateProvidedModules();
    }

    public void setModuleDescriptorFactory(ModuleDescriptorFactory moduleDescriptorFactory) {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
    }

    @Override
    public Object getProvidedModules() {
        return this.providedModules;
    }

    private void evaluateProvidedModules() {
        this.providedModules = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, getInstance(), PROVIDED_MODULES);
    }

    @Override
    public void doWithDynamicModules() {
        if (getInstance() instanceof DynamicPlugin) {
            DynamicPlugin dynamicPlugin = (DynamicPlugin) getInstance();
            Closure dynamicModules = dynamicPlugin.doWithDynamicModules();
            if (dynamicModules != null) {
                dynamicModules.setResolveStrategy(Closure.DELEGATE_FIRST);
                dynamicModules.setDelegate(this);
                dynamicModules.call();
            }
        }
    }

    @Override
    public void addModuleDescriptor(String type, Map<String, Object> args) {
        addModuleDescriptor(type, args, null);
    }

    @Override
    public void addModuleDescriptor(String type, Map<String, Object> args, Closure<?> closure) {
        try {
            ModuleDescriptor<?> moduleDescriptor = this.moduleDescriptorFactory.getModuleDescriptor(type);
            moduleDescriptor.init(this, args);
            if (closure != null) {
                closure.setDelegate(moduleDescriptor);
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                closure.call();
            }
            this.modules.put(moduleDescriptor.getKey(), moduleDescriptor);
        }
        catch (ClassNotFoundException e) {
            logger.error("Unable to get module description of '" + type + "'", e);
        }
    }

    @Override
    public Collection<ModuleDescriptor<?>> getModuleDescriptors() {
        return this.modules.values();
    }

    @Override
    public ModuleDescriptor<?> getModuleDescriptor(String key) {
        return this.modules.get(key);
    }

    @Override
    public <M> List<ModuleDescriptor<M>> getModuleDescriptorsByModuleClass(Class<M> aClass) {
        List<ModuleDescriptor<M>> result = new ArrayList<>();
        for (ModuleDescriptor<?> moduleDescriptor : this.modules.values()) {
            Class<?> moduleClass = moduleDescriptor.getModuleClass();
            if (moduleClass != null && aClass.isAssignableFrom(moduleClass)) {
                @SuppressWarnings("unchecked")
                ModuleDescriptor<M> typedModuleDescriptor = (ModuleDescriptor<M>) moduleDescriptor;
                result.add(typedModuleDescriptor);
            }
        }
        return result;
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        Object[] array = (Object[]) args;
        if (array.length > 0) {
            if (array.length > 1) {
                addModuleDescriptor(name, (Map<String, Object>) array[0], (Closure) array[1]);
            }
            else {
                addModuleDescriptor(name, (Map<String, Object>) array[0]);
            }
        }
        return true;
    }

}
