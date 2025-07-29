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
package grails.plugins;

import org.springframework.context.ApplicationContextAware;

import grails.plugins.exceptions.PluginException;

/**
 * The factory to manage {@link ModuleDescriptor}
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public interface ModuleDescriptorFactory extends ApplicationContextAware {

    void addModuleDescriptor(String type, Class<? extends ModuleDescriptor<?>> moduleDescriptorClass);

    <M> ModuleDescriptor<M> getModuleDescriptor(String type) throws PluginException, ClassNotFoundException;

    <M> Class<? extends ModuleDescriptor<M>> getModuleDescriptorClass(String type);

    boolean hasModuleDescriptor(String type);

}
