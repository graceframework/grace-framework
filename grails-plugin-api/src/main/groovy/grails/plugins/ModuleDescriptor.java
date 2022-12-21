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

import java.util.Map;

import grails.plugins.exceptions.PluginException;

/**
 * Descriptor for dynamic Module.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public interface ModuleDescriptor<T> {

    String getKey();

    String getPluginKey();

    String getCompleteKey();

    String getName();

    String getDescription();

    String getI18nNameKey();

    String getDescriptionKey();

    Map<String, String> getParams();

    Class<T> getModuleClass();

    T getModule();

    void init(DynamicGrailsPlugin plugin, Map<String, ?> args) throws PluginException;

    void destroy(DynamicGrailsPlugin plugin);

    DynamicGrailsPlugin getPlugin();

    void enabled();

    void disabled();

    boolean isEnabled();

}
