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
package org.grails.plugins.admin

import grails.plugins.descriptors.WebItemModuleDescriptor
import grails.plugins.descriptors.WebSectionModuleDescriptor
import grails.plugins.DynamicPlugin
import grails.util.GrailsUtil

/**
 * Admin Console for {@link DynamicPlugin}.
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
class AdminConsoleGrailsPlugin extends DynamicPlugin {

    def version = GrailsUtil.getGrailsVersion()
    def grailsVersion = '2024.0.0 > *'
    // def dependsOn = [dynamicModules: '*']
    def loadAfter = ['dynamicModules', 'urlMappings']

    def providedModules = [
            WebItemModuleDescriptor,
            WebSectionModuleDescriptor
    ]

    def title = "Grace Admin Console"
    def description = '''\
Grace Admin Console is a powerful and flexible, extensible administration framework and management console.
'''

    Closure doWithDynamicModules() { {->
        // Admin Console Navigation Bar, Tabs, Sidebar
        webSection(key: "admin.navigation.bar", name: "Admin Navigation Bar", i18nNameKey: "admin.menu.section.navigation.bar")

        webSection(key: "admin.tabs.dashboard", name:"Dashboard Tab", location:"admin.navigation.bar", i18nNameKey: "admin.menu.section.dashboard.name", descriptionKey: "admin.menu.section.dashboard.desc", weight: 1)

            webItem(key: "admin_summary", name: "Admin Dashboard Link", section: "admin.tabs.dashboard", i18nNameKey: "admin.menu.item.admin.summary", weight: 10) {
                label(key: "menu.admin.dashboard")
                link(linkId: "admin_summary", url: [namespace: 'admin', controller: 'dashboard', action: 'index'])
            }

        webSection(key: "admin.tabs.system", name: "System", location: "admin.navigation.bar", i18nNameKey: "admin.menu.section.system.name", descriptionKey: "admin.menu.section.system.desc", weight: 1000)

            webSection(key: "admin.sidebars.system", name: "System Sidebar", location: "admin.tabs.system", i18nNameKey: "admin.menu.sidebars.system.name", descriptionKey: "admin.menu.sidebars.system.desc")

                webItem(key: "systeminfo", name: "System Info", section: "admin.tabs.system/admin.sidebars.system", i18nNameKey: "admin.menu.item.systeminfo.name", weight: 10) {
                    label(key: "admin.menu.system.system.info")
                    link(linkId: "system_info", url: [namespace: 'admin', controller: 'system', action: 'index'])
                }
                webItem(key: "pluginlist", name: "Plugin List", section: "admin.tabs.system/admin.sidebars.system", i18nNameKey: "admin.menu.item.pluginlist.name", weight: 20) {
                    label(key: "admin.menu.system.plugin.list")
                    link(linkId: "plugin_list", url: [namespace: 'admin', controller: 'plugin', action: 'index'])
                }

            webSection(key: "admin.sidebars.actuator", name: "Actuator", location: "admin.tabs.system", i18nNameKey: "admin.menu.sidebars.actuator.name", descriptionKey: "admin.menu.sidebars.actuator.desc")

                webItem(key: "actuatorindex", name: "Endpoints", section: "admin.tabs.system/admin.sidebars.actuator", i18nNameKey: "admin.menu.item.actuatorindex.name", weight: 10) {
                    label(key: "admin.menu.system.actuator.index")
                    link(linkId: "actuator_index", url: [namespace: 'admin', controller: 'actuator', action: 'index'])
                }
                webItem(key: "actuatorinfo", name: "Info", section: "admin.tabs.system/admin.sidebars.actuator", i18nNameKey: "admin.menu.item.actuatorinfo.name", weight: 20) {
                    label(key: "admin.menu.system.actuator.info")
                    link(linkId: "actuator_info", url: [namespace: 'admin', controller: 'actuator', action: 'info'])
                }
                webItem(key: "actuatorhealth", name: "Health", section: "admin.tabs.system/admin.sidebars.actuator", i18nNameKey: "admin.menu.item.actuatorhealth.name", weight: 30) {
                    label(key: "admin.menu.system.actuator.health")
                    link(linkId: "actuator_health", url: [namespace: 'admin', controller: 'actuator', action: 'health'])
                }
                webItem(key: "actuatorbeans", name: "Beans", section: "admin.tabs.system/admin.sidebars.actuator", i18nNameKey: "admin.menu.item.actuatorbeans.name", weight: 40) {
                    label(key: "admin.menu.system.actuator.beans")
                    link(linkId: "actuator_beans", url: [namespace: 'admin', controller: 'actuator', action: 'beans'])
                }
                webItem(key: "actuatorenv", name: "Environment", section: "admin.tabs.system/admin.sidebars.actuator", i18nNameKey: "admin.menu.item.actuatorenv.name", weight: 50) {
                    label(key: "admin.menu.system.actuator.env")
                    link(linkId: "actuator_env", url: [namespace: 'admin', controller: 'actuator', action: 'env'])
                }
                webItem(key: "actuatorloggers", name: "Loggers", section: "admin.tabs.system/admin.sidebars.actuator", i18nNameKey: "admin.menu.item.actuatorloggers.name", weight: 60) {
                    label(key: "admin.menu.system.actuator.loggers")
                    link(linkId: "actuator_loggers", url: [namespace: 'admin', controller: 'actuator', action: 'loggers'])
                }
    }}

}
