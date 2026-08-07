/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.plugins.modules

import groovy.transform.CompileStatic

import grails.plugins.descriptors.WebItemModuleDescriptor
import grails.plugins.descriptors.WebSectionModuleDescriptor
import grails.plugins.Plugin
import grails.util.GrailsUtil

/**
 * Dynamic Modules Plugin offer new ways of creating modular and maintainable applications.
 *
 * @author Michael Yan
 * @since 2022.1.0
 */
@CompileStatic
class DynamicModulesGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def grailsVersion = "2024.0.0 > *"
    def title = 'Grace Dynamic Modules Plugin'
    def description = '''\
Grace Dynamic Modules Plugin offer new ways of creating modular and maintainable Grace applications.
'''

    def providedModules = [
            WebItemModuleDescriptor,
            WebSectionModuleDescriptor
    ]

}
