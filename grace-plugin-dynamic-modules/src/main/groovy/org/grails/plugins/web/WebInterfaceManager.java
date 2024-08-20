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
package org.grails.plugins.web;

import java.util.List;
import java.util.Map;

import grails.plugins.descriptors.WebItemModuleDescriptor;
import grails.plugins.descriptors.WebSectionModuleDescriptor;

/**
 * WebInterfaceManager
 *
 * @author Michael Yan
 * @since 2022.1.0
 * @deprecated since 2023.0.0, in favor of org.graceframework.plugins:dynamic-modules
 */
@Deprecated(since = "2023.0.0")
public interface WebInterfaceManager {

    List<WebSectionModuleDescriptor> getSections(String location);

    List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String, Object> context);

    List<WebItemModuleDescriptor> getItems(String section);

    List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String, Object> context);

}
