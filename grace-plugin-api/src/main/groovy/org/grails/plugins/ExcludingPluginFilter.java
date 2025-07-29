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
import java.util.List;
import java.util.Set;

import grails.plugins.GrailsPlugin;

/**
 * Implementation of <code>PluginFilter</code> which removes that all of the supplied
 * plugins (identified by name) as well as their dependencies are omitted from the
 * filtered plugin list.
 *
 * @author Phil Zoio
 */
public class ExcludingPluginFilter extends BasePluginFilter {

    public ExcludingPluginFilter(Set<String> excluded) {
        super(excluded);
    }

    public ExcludingPluginFilter(String[] excluded) {
        super(excluded);
    }

    @Override
    protected List<GrailsPlugin> getPluginList(List<GrailsPlugin> original, List<GrailsPlugin> pluginList) {
        // go through and remove ones that don't apply
        List<GrailsPlugin> newList = new ArrayList<>(original);
        // remove the excluded dependencies
        newList.removeIf(pluginList::contains);

        return newList;
    }

    @Override
    protected void addPluginDependencies(List<GrailsPlugin> additionalList, GrailsPlugin plugin) {
        // find the plugins which depend on the one we've excluded
        String pluginName = plugin.getName();

        Collection<GrailsPlugin> values = getAllPlugins();
        for (GrailsPlugin p : values) {
            // ignore the current plugin
            if (pluginName.equals(p.getName())) {
                continue;
            }

            boolean depends = isDependentOn(p, pluginName);

            if (depends) {
                registerDependency(additionalList, p);
            }
        }
    }

}
