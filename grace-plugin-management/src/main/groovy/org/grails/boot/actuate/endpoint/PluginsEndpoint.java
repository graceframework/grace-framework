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
package org.grails.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;

/**
 * {@link Endpoint @Endpoint} to expose details of an Grails application's plugins.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@Endpoint(id = "plugins")
public class PluginsEndpoint {

    private final GrailsPluginManager pluginManager;

    public PluginsEndpoint(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @ReadOperation
    public ApplicationPlugins plugins() {
        GrailsPlugin[] allPlugins = this.pluginManager.getAllPlugins();

        List<PluginDescriptor> plugins = new ArrayList<>();
        for (GrailsPlugin plugin : allPlugins) {
            plugins.add(new PluginDescriptor(plugin.getName(), plugin.getInstance().getClass(),
                    plugin.getVersion(), plugin.getDependencyNames()));
        }

        return new ApplicationPlugins(plugins);
    }

    public static final class ApplicationPlugins {

        private final List<PluginDescriptor> plugins;

        public ApplicationPlugins(List<PluginDescriptor> plugins) {
            this.plugins = plugins;
        }

        public List<PluginDescriptor> getPlugins() {
            return this.plugins;
        }

    }

    public static final class PluginDescriptor {

        private final String name;

        private final Class<?> type;

        private final String version;

        private final String[] dependencies;

        public PluginDescriptor(String name, Class<?> type, String version, String[] dependencies) {
            this.name = name;
            this.type = type;
            this.version = version;
            this.dependencies = dependencies;
        }

        public String getName() {
            return this.name;
        }

        public Class<?> getType() {
            return this.type;
        }

        public String getVersion() {
            return this.version;
        }

        public String[] getDependencies() {
            return this.dependencies;
        }

    }

}
