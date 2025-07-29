/*
 * Copyright 2022-2024 the original author or authors.
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
package org.grails.plugins.web

import groovy.transform.CompileStatic
import grails.plugins.descriptors.WebItemModuleDescriptor
import grails.plugins.descriptors.WebSectionModuleDescriptor

import grails.plugins.GrailsPluginManager
import grails.plugins.PluginManagerAware

/**
 * DefaultWebInterfaceManager
 *
 * @author Michael Yan
 * @since 2022.1.0
 * @deprecated since 2023.0.0, in favor of org.graceframework.plugins:dynamic-modules
 */
@CompileStatic

class DefaultWebInterfaceManager implements WebInterfaceManager, PluginManagerAware {

    private GrailsPluginManager pluginManager

    DefaultWebInterfaceManager() {
    }

    DefaultWebInterfaceManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
    }

    @Override
    void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
    }

    @Override
    List<WebSectionModuleDescriptor> getSections(String location) {
        if (location == null) {
            return Collections.emptyList()
        }

        List<WebSectionModuleDescriptor> result = new ArrayList<>()

        List<WebSectionModuleDescriptor> descriptors = pluginManager.getEnabledModuleDescriptorsByClass(WebSectionModuleDescriptor)
        descriptors.sort { it.weight }
        for (WebSectionModuleDescriptor descriptor : descriptors) {
            if (location.equalsIgnoreCase(descriptor.getLocation()) && descriptor.isEnabled()) {
                result.add(descriptor)
            }
        }

        result
    }

    @Override
    List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String, Object> context) {
        getSections(location)
    }

    @Override
    List<WebItemModuleDescriptor> getItems(String section) {
        if (section == null) {
            return Collections.emptyList()
        }

        List<WebItemModuleDescriptor> result = new ArrayList<>()

        List<WebItemModuleDescriptor> descriptors = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor)
        for (WebItemModuleDescriptor descriptor : descriptors) {
            if (section.equalsIgnoreCase(descriptor.getSection()) && descriptor.isEnabled()) {
                result.add(descriptor)
            }
        }

        result
    }

    @Override
    List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String, Object> context) {
        getItems(section)
    }

}
