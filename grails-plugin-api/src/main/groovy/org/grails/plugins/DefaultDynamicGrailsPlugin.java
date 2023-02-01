/*
 * Copyright 2021-2023 the original author or authors.
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
import org.springframework.core.io.Resource;

import grails.core.GrailsApplication;
import grails.plugins.DynamicGrailsPlugin;
import grails.plugins.DynamicPlugin;
import grails.plugins.ModuleDescriptor;
import grails.plugins.ModuleDescriptorFactory;
import grails.util.GrailsClassUtils;

/**
 * Default implementation of {@link DynamicGrailsPlugin}
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class DefaultDynamicGrailsPlugin extends DefaultGrailsPlugin implements DynamicGrailsPlugin {

    private final Map<String, ModuleDescriptor<?>> modules = new LinkedHashMap<>();

    private ModuleDescriptorFactory moduleDescriptorFactory;

    private Class<?>[] providedModules = {};

    public DefaultDynamicGrailsPlugin(Class<?> pluginClass, Resource resource, GrailsApplication application) {
        super(pluginClass, resource, application);

        evaluateProvidedModules();
    }

    public DefaultDynamicGrailsPlugin(Class<?> pluginClass, GrailsApplication application) {
        super(pluginClass, application);

        evaluateProvidedModules();
    }

    public void setModuleDescriptorFactory(ModuleDescriptorFactory moduleDescriptorFactory) {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
    }

    public Class<?>[] getProvidedModules() {
        return this.providedModules;
    }

    @SuppressWarnings("unchecked")
    private void evaluateProvidedModules() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.pluginBean, getInstance(), PROVIDED_MODULES);
        if (result instanceof Collection) {
            Collection<Class<?>> moduleList = (Collection<Class<?>>) result;
            this.providedModules = (Class<?>[]) moduleList.toArray(new Class[0]);
        }
    }

    @Override
    public void doWithDynamicModules() {
        if (getInstance() instanceof DynamicPlugin) {
            DynamicPlugin dynamicPlugin = (DynamicPlugin) getInstance();
            dynamicPlugin.doWithDynamicModules();
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
                closure.setResolveStrategy(Closure.DELEGATE_ONLY);
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

}
