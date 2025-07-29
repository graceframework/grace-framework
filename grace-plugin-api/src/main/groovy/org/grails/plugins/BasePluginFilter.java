/*
 * Copyright 2004-2022 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import grails.plugins.GrailsPlugin;
import grails.plugins.PluginFilter;

/**
 * Base functionality shared by <code>IncludingPluginFilter</code> and
 * <code>ExcludingPluginFilter</code>.
 *
 * @author Phil Zoio
 */
public abstract class BasePluginFilter implements PluginFilter {

    /**
     * The supplied included plugin names (a String).
     */
    private final Set<String> suppliedNames = new HashSet<>();

    /**
     * Plugins corresponding with the supplied names.
     */
    private final List<GrailsPlugin> explicitlyNamedPlugins = new ArrayList<>();

    /**
     * Plugins derivied through a dependency relationship.
     */
    private final List<GrailsPlugin> derivedPlugins = new ArrayList<>();

    /**
     * Holds a name to GrailsPlugin map (String, Plugin).
     */
    protected final Map<String, GrailsPlugin> nameMap = new HashMap<>();

    /**
     * Temporary field holding list of plugin names added to the filtered List
     * to return (String).
     */
    private final Set<String> addedNames = new HashSet<>();

    private List<GrailsPlugin> originalPlugins;

    public BasePluginFilter(Set<String> suppliedNames) {
        this.suppliedNames.addAll(suppliedNames);
    }

    public BasePluginFilter(String[] included) {
        for (String s : included) {
            this.suppliedNames.add(s.trim());
        }
    }

    /**
     * Defines operation for adding dependencies for a plugin to the list
     */
    protected abstract void addPluginDependencies(List<GrailsPlugin> additionalList, GrailsPlugin plugin);

    /**
     * Defines an operation getting the final list to return from the original
     * and derived lists
     * @return a sublist containing the elements of the original list
     *         corresponding with the explicitlyNamed items as passed into the constructor
     */
    protected abstract List<GrailsPlugin> getPluginList(List<GrailsPlugin> original, List<GrailsPlugin> pluginList);

    /**
     * Template method shared by subclasses of <code>BasePluginFilter</code>.
     */
    public List<GrailsPlugin> filterPluginList(List<GrailsPlugin> original) {
        this.originalPlugins = Collections.unmodifiableList(original);

        buildNameMap();
        buildExplicitlyNamedList();
        buildDerivedPluginList();

        List<GrailsPlugin> pluginList = new ArrayList<>();
        pluginList.addAll(this.explicitlyNamedPlugins);
        pluginList.addAll(this.derivedPlugins);

        return getPluginList(this.originalPlugins, pluginList);
    }

    /**
     * Builds list of <code>GrailsPlugins</code> which are derived from the
     * <code>explicitlyNamedPlugins</code> through a dependency relationship
     */
    private void buildDerivedPluginList() {
        // find their dependencies
        for (GrailsPlugin plugin : this.explicitlyNamedPlugins) {
            // recursively add in plugin dependencies
            addPluginDependencies(this.derivedPlugins, plugin);
        }
    }

    /**
     * Checks whether a plugin is dependent on another plugin with the specified
     * name
     *
     * @param plugin
     *            the plugin to compare
     * @param pluginName
     *            the name to compare against
     * @return true if <code>plugin</code> depends on <code>pluginName</code>
     */
    protected boolean isDependentOn(GrailsPlugin plugin, String pluginName) {
        // check if toCompare depends on the current plugin
        String[] dependencyNames = plugin.getDependencyNames();
        for (final String dependencyName : dependencyNames) {
            if (pluginName.equals(dependencyName)) {
                // we've establish that p does depend on plugin, so we can
                // break from this loop
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the sublist of the supplied set who are explicitly named, either
     * as included or excluded plugins
     *
     */
    private void buildExplicitlyNamedList() {
        // each plugin must either be in included set or must be a dependent of
        // included set
        for (GrailsPlugin plugin : this.originalPlugins) {
            // find explicitly included plugins
            String name = plugin.getName();
            if (this.suppliedNames.contains(name)) {
                this.explicitlyNamedPlugins.add(plugin);
                this.addedNames.add(name);
            }
        }
    }

    /**
     * Builds a name to plugin map from the original list of plugins supplied
     *
     */
    private void buildNameMap() {
        for (GrailsPlugin plugin : this.originalPlugins) {
            this.nameMap.put(plugin.getName(), plugin);
        }
    }

    /**
     * Adds a plugin to the additional if this hasn't happened already
     */
    protected void registerDependency(List<GrailsPlugin> additionalList, GrailsPlugin plugin) {
        if (!this.addedNames.contains(plugin.getName())) {
            this.addedNames.add(plugin.getName());
            additionalList.add(plugin);
            addPluginDependencies(additionalList, plugin);
        }
    }

    protected Collection<GrailsPlugin> getAllPlugins() {
        return Collections.unmodifiableCollection(this.nameMap.values());
    }

    protected GrailsPlugin getNamedPlugin(String name) {
        return this.nameMap.get(name);
    }

    protected Set<String> getSuppliedNames() {
        return this.suppliedNames;
    }

}
