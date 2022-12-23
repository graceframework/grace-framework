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
package grails.plugins;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;

/**
 * Dynamic plugins to implement.
 * Plugin implementations should define the plugin hooks doWithDynamicModules
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public interface DynamicGrailsPlugin extends GrailsPlugin {

    String PROVIDED_MODULES = "providedModules";

    String DO_WITH_DYNAMIC_MODULES = "doWithDynamicModules";

    Class<?>[] getProvidedModules();

    void doWithDynamicModules();

    Collection<ModuleDescriptor<?>> getModuleDescriptors();

    ModuleDescriptor<?> getModuleDescriptor(String key);

    <M> List<ModuleDescriptor<M>> getModuleDescriptorsByModuleClass(Class<M> moduleClass);

    void addModuleDescriptor(String type, Map<String, Object> args);

    void addModuleDescriptor(String type, Map<String, Object> args, Closure<?> closure);
}
