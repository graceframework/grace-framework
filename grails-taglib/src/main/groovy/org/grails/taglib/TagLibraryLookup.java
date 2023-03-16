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
package org.grails.taglib;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.GroovyObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.core.GrailsTagLibClass;
import grails.core.support.GrailsApplicationAware;

import org.grails.core.artefact.TagLibArtefactHandler;
import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.taglib.encoder.WithCodecHelper;

/**
 * Looks up tag library instances.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class TagLibraryLookup implements ApplicationContextAware, GrailsApplicationAware, InitializingBean, SmartInitializingSingleton {

    protected ApplicationContext applicationContext;

    protected GrailsApplication grailsApplication;

    protected Map<String, Map<String, Object>> tagNamespaces = new HashMap<>();

    protected Map<String, NamespacedTagDispatcher> namespaceDispatchers = new HashMap<>();

    protected Map<String, Set<String>> tagsThatReturnObjectForNamespace = new HashMap<>();

    protected Map<String, Map<String, Map<String, Object>>> encodeAsForTagNamespaces = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.grailsApplication == null || this.applicationContext == null) {
            return;
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            registerTagLibraries();
        }
        catch (GrailsConfigurationException e) {
            // ignore exception
        }
        registerTemplateNamespace();

        registerNamespaceDispatchers();
    }

    private void registerNamespaceDispatchers() {
        for (String namespace : this.tagNamespaces.keySet()) {
            registerNamespaceDispatcher(namespace);
        }
    }

    protected void registerNamespaceDispatcher(String namespace) {
        this.namespaceDispatchers.put(namespace, new NamespacedTagDispatcher(namespace, null, this.grailsApplication, this));
    }

    protected void registerTagLibraries() {
        GrailsClass[] taglibs = this.grailsApplication.getArtefacts(TagLibArtefactHandler.TYPE);
        for (GrailsClass grailsClass : taglibs) {
            registerTagLib((GrailsTagLibClass) grailsClass, true);
        }
    }

    protected void registerTemplateNamespace() {
        this.namespaceDispatchers.put(TemplateNamespacedTagDispatcher.TEMPLATE_NAMESPACE,
                new TemplateNamespacedTagDispatcher(null, this.grailsApplication, this));
    }

    /**
     * Registers a tag library for lookup. Each of the tags in the
     * library is mapped by namespace:name to the taglib bean. If the
     * taglib has already been registered, this method will override
     * the existing information and update the tags to use the new
     * version.
     * @param taglib The taglib descriptor class.
     */
    public void registerTagLib(GrailsTagLibClass taglib) {
        registerTagLib(taglib, false);
    }

    private void registerTagLib(GrailsTagLibClass taglib, boolean isInitialization) {
        String namespace = taglib.getNamespace();

        if (!isInitialization) {
            registerNamespaceDispatcher(namespace);
        }
        Set<String> tagsThatReturnObject = this.tagsThatReturnObjectForNamespace.get(namespace);
        if (tagsThatReturnObject == null) {
            tagsThatReturnObject = new HashSet<>();
            this.tagsThatReturnObjectForNamespace.put(namespace, tagsThatReturnObject);
        }
        Map<String, Object> tags = this.tagNamespaces.get(namespace);
        if (tags == null) {
            tags = new HashMap<>();
            this.tagNamespaces.put(namespace, tags);
        }

        for (String tagName : taglib.getTagNames()) {
            putTagLib(tags, tagName, taglib);
            tagsThatReturnObject.remove(tagName);
        }
        for (String tagName : taglib.getTagNamesThatReturnObject()) {
            tagsThatReturnObject.add(tagName);
        }

        Map<String, Map<String, Object>> encodeAsForTagNamespace = this.encodeAsForTagNamespaces.get(namespace);
        if (encodeAsForTagNamespace == null) {
            encodeAsForTagNamespace = new HashMap<>();
            this.encodeAsForTagNamespaces.put(namespace, encodeAsForTagNamespace);
        }

        Map<String, Object> defaultEncodeAsForTagLib = null;
        if (taglib.getDefaultEncodeAs() != null) {
            defaultEncodeAsForTagLib = Collections.unmodifiableMap(WithCodecHelper.makeSettingsCanonical(taglib.getDefaultEncodeAs()));
        }

        for (String tagName : taglib.getTagNames()) {
            Object codecInfo = taglib.getEncodeAsForTag(tagName);
            Map<String, Object> codecInfoMap = WithCodecHelper.mergeSettingsAndMakeCanonical(codecInfo, defaultEncodeAsForTagLib);
            if (codecInfoMap != null) {
                encodeAsForTagNamespace.put(tagName, codecInfoMap);
            }
        }
    }

    protected void putTagLib(Map<String, Object> tags, String name, GrailsTagLibClass taglib) {
        tags.put(name, this.applicationContext.getBean(taglib.getFullName()));
    }

    /**
     * Looks up a tag library for the given namespace and tag name.
     *
     * @param namespace The tag library namespace
     * @param tagName The tag name
     * @return The tag library or null if it wasn't found
     */
    public GroovyObject lookupTagLibrary(String namespace, String tagName) {
        Map<String, Object> tags = this.tagNamespaces.get(namespace);
        if (tags == null) {
            return null;
        }
        return (GroovyObject) tags.get(tagName);
    }

    public boolean doesTagReturnObject(String namespace, String tagName) {
        Set<String> tagsThatReturnObject = this.tagsThatReturnObjectForNamespace.get(namespace);
        return tagsThatReturnObject != null && tagsThatReturnObject.contains(tagName);
    }

    public Map<String, Object> getEncodeAsForTag(String namespace, String tagName) {
        Map<String, Map<String, Object>> encodeAsForTagNamespace = this.encodeAsForTagNamespaces.get(namespace);
        return encodeAsForTagNamespace != null ? encodeAsForTagNamespace.get(tagName) : null;
    }

    /**
     * Looks up a namespace dispatcher for the given namespace
     * @param namespace The namespace
     * @return The NamespacedTagDispatcher
     */
    public NamespacedTagDispatcher lookupNamespaceDispatcher(String namespace) {
        return this.namespaceDispatchers.get(namespace);
    }

    /**
     * Returns whether the given namespace is in use
     * @param namespace The namespace
     * @return true if it is in use
     */
    public boolean hasNamespace(String namespace) {
        return this.namespaceDispatchers.containsKey(namespace);
    }

    /**
     * @return The namespaces available
     */
    public Set<String> getAvailableNamespaces() {
        return this.namespaceDispatchers.keySet();
    }

    public Set<String> getAvailableTags(String namespace) {
        Map<String, Object> tags = this.tagNamespaces.get(namespace);
        if (tags == null) {
            return Collections.emptySet();
        }
        return tags.keySet();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

}
