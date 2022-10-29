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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import groovy.lang.GroovyClassLoader;
import org.springframework.util.Assert;

import grails.core.GrailsApplication;
import grails.plugins.GrailsPlugin;
import grails.plugins.exceptions.PluginException;

/**
 * @author Graeme Rocher
 * @since 0.4
 */
public class MockGrailsPluginManager extends AbstractGrailsPluginManager {

    private boolean checkForChangesExpected = false;

    public MockGrailsPluginManager(GrailsApplication application) {
        super(application);
        loadPlugins();
    }

    public MockGrailsPluginManager() {
        this(new MockGrailsApplication(new Class[0], new GroovyClassLoader()));
    }

    @Override
    public GrailsPlugin getGrailsPlugin(String name) {
        return plugins.get(name);
    }

    public GrailsPlugin getGrailsPlugin(String name, BigDecimal version) {
        return plugins.get(name);
    }

    @Override
    public boolean hasGrailsPlugin(String name) {
        return plugins.containsKey(name);
    }

    public void registerMockPlugin(GrailsPlugin plugin) {
        plugin.setManager(this);
        plugins.put(plugin.getName(), plugin);
        pluginList.add(plugin);
    }

    public GrailsPlugin[] getUserPlugins() {
        return getAllPlugins();
    }

    public void loadPlugins() throws PluginException {
        initialised = true;
    }

    public void checkForChanges() {
        Assert.isTrue(this.checkForChangesExpected, "checkForChangesExpected must be true");
        this.checkForChangesExpected = false;
    }

    @Override
    public boolean isInitialised() {
        return true;
    }

    public void refreshPlugin(String name) {
        GrailsPlugin plugin = plugins.get(name);
        if (plugin != null) {
            plugin.refresh();
        }
    }

    public Collection<GrailsPlugin> getPluginObservers(GrailsPlugin plugin) {
        throw new UnsupportedOperationException(
                "The class [MockGrailsPluginManager] doesn't support the method getPluginObservers");
    }

    public void informObservers(String pluginName, Map<String, Object> event) {
        // do nothing
    }

    public void expectCheckForChanges() {
        Assert.state(!this.checkForChangesExpected, "checkForChangesExpected must be false");
        this.checkForChangesExpected = true;
    }

    public void verify() {
        Assert.state(!this.checkForChangesExpected, "checkForChangesExpected must be false");
    }

}
