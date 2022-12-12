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
package org.grails.plugins;

import java.util.Map;

import grails.plugins.DynamicGrailsPlugin;
import grails.plugins.ModuleDescriptor;
import grails.plugins.exceptions.PluginException;

public class AbstractModuleDescriptor<T> implements ModuleDescriptor<T> {

    protected DynamicGrailsPlugin plugin;

    protected String key;

    protected String name;

    protected String description;

    protected String moduleClassName;

    protected Class<T> moduleClass;

    private boolean enabled = false;

    private Map<String, String> params;

    private String i18nNameKey;

    private String completeKey;

    private String descriptionKey;

    public AbstractModuleDescriptor() {
    }

    @Override
    public void init(final DynamicGrailsPlugin plugin, final Map<String, ?> args) throws PluginException {
        this.plugin = plugin;
        this.key = String.valueOf(args.get("key"));
//        this.completeKey = buildCompleteKey(plugin, this.key);
        this.name = String.valueOf(args.get("name"));
        this.i18nNameKey = String.valueOf(args.get("i18nNameKey"));
        this.description = String.valueOf(args.get("description"));
        this.moduleClassName = String.valueOf(args.get("class"));
//        this.params = LoaderUtils.getParams(element);
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getPluginKey() {
        return this.plugin.getFileSystemShortName() + "-" + this.key;
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

    }

    @Override
    public void disabled() {

    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ModuleDescriptor: [")
                .append("\n    key: ").append(key)
                .append("\n    name: ").append(name)
                .append("\n    i18nNameKey: ").append(i18nNameKey)
                .append("\n    description: ").append(description)
                .append("\n    moduleClassName: ").append(moduleClassName)
                .append("]");
        return sb.toString();
    }

}
