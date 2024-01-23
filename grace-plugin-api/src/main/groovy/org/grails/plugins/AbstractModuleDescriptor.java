/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import grails.plugins.DynamicGrailsPlugin;
import grails.plugins.ModuleDescriptor;
import grails.plugins.exceptions.PluginException;

/**
 * Abstract ModuleDescriptor
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class AbstractModuleDescriptor<T> implements ModuleDescriptor<T> {

    protected DynamicGrailsPlugin plugin;

    protected String key;

    protected String name;

    protected String description;

    protected String moduleClassName;

    protected Class<T> moduleClass;

    private boolean enabled = true;

    private Map<String, String> params;

    private String i18nNameKey;

    private String completeKey;

    private String descriptionKey;

    public AbstractModuleDescriptor() {
    }

    @Override
    public void init(final DynamicGrailsPlugin plugin, final Map<String, ?> args) throws PluginException {
        this.plugin = plugin;
        this.key = args.get("key") != null ? args.get("key").toString() : null;
        this.completeKey = buildCompleteKey(plugin, this.key);
        this.name = args.get("name") != null ? args.get("name").toString() : null;
        this.i18nNameKey = args.get("i18nNameKey") != null ? args.get("i18nNameKey").toString() : null;
        this.description = args.get("description") != null ? args.get("description").toString() : null;
        this.descriptionKey = args.get("descriptionKey") != null ? args.get("descriptionKey").toString() : null;
        this.moduleClassName = args.get("class") != null ? args.get("class").toString() : null;
        if (args.get("enabled") != null) {
            this.enabled = Boolean.parseBoolean(StringUtils.defaultString(args.get("enabled").toString(), "true"));
        }
        this.params = new HashMap<>();
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getPluginKey() {
        return this.plugin.getFileSystemShortName();
    }

    @Override
    public String getCompleteKey() {
        return this.completeKey;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getI18nNameKey() {
        return this.i18nNameKey;
    }

    @Override
    public String getDescriptionKey() {
        return this.descriptionKey;
    }

    @Override
    public Map<String, String> getParams() {
        return this.params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public Class<T> getModuleClass() {
        return this.moduleClass;
    }

    @Override
    public T getModule() {
        return null;
    }

    @Override
    public void destroy(DynamicGrailsPlugin plugin) {

    }

    @Override
    public DynamicGrailsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public void enabled() {
        this.enabled = true;
    }

    @Override
    public void disabled() {
        this.enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    private String buildCompleteKey(final DynamicGrailsPlugin plugin, final String moduleKey) {
        if (plugin == null) {
            return null;
        }

        return plugin.getFileSystemShortName() + ":" + moduleKey;
    }

    @Override
    public String toString() {
        return getCompleteKey() + " (" + getDescription() + ")";
    }

}
