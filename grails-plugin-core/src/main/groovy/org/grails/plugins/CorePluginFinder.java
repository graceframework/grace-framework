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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import grails.core.GrailsApplication;
import grails.core.support.ParentApplicationContextAware;

import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.io.support.SpringIOUtils;

/**
 * Loads core plugin classes. Contains functionality moved in from <code>DefaultGrailsPluginManager</code>.
 *
 * @author Graeme Rocher
 * @author Phil Zoio
 */
public class CorePluginFinder implements ParentApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(CorePluginFinder.class);

    public static final String CORE_PLUGIN_PATTERN = "META-INF/grails-plugin.xml";

    private final Set<Class<?>> foundPluginClasses = new HashSet<>();

    @SuppressWarnings("unused")
    private final GrailsApplication application;

    @SuppressWarnings("rawtypes")
    private final Map<Class, BinaryGrailsPluginDescriptor> binaryDescriptors = new HashMap<>();

    public CorePluginFinder(GrailsApplication application) {
        this.application = application;
    }

    public Class<?>[] getPluginClasses() {

        // just in case we try to use this twice
        this.foundPluginClasses.clear();

        try {
            Resource[] resources = resolvePluginResources();
            if (resources.length > 0) {
                loadCorePluginsFromResources(resources);
            }
            else {
                throw new IllegalStateException("Grails was unable to load plugins dynamically. " +
                        "This is normally a problem with the container class loader configuration, see troubleshooting and FAQ for more info. ");
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("WARNING: I/O exception loading core plugin dynamically, attempting static load. " +
                    "This is usually due to deployment onto containers with unusual classloading setups. Message: " + e.getMessage());
        }
        return this.foundPluginClasses.toArray(new Class[0]);
    }

    public BinaryGrailsPluginDescriptor getBinaryDescriptor(Class<?> pluginClass) {
        return this.binaryDescriptors.get(pluginClass);
    }

    private Resource[] resolvePluginResources() throws IOException {
        Enumeration<URL> resources = this.application.getClassLoader().getResources(CORE_PLUGIN_PATTERN);
        List<Resource> resourceList = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            resourceList.add(new UrlResource(url));
        }
        return resourceList.toArray(new Resource[0]);
    }

    @SuppressWarnings("rawtypes")
    private void loadCorePluginsFromResources(Resource[] resources) throws IOException {
        try {
            SAXParser saxParser = SpringIOUtils.newSAXParser();
            for (Resource resource : resources) {
                InputStream input = null;

                try {
                    input = resource.getInputStream();
                    PluginHandler ph = new PluginHandler();
                    saxParser.parse(input, ph);

                    for (String pluginType : ph.pluginTypes) {
                        Class<?> pluginClass = attemptCorePluginClassLoad(pluginType);
                        if (pluginClass != null) {
                            addPlugin(pluginClass);
                            this.binaryDescriptors.put(pluginClass, new BinaryGrailsPluginDescriptor(resource, ph.pluginClasses));
                        }
                    }

                }
                finally {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        }
        catch (ParserConfigurationException | SAXException e) {
            throw new GrailsConfigurationException("XML parsing error loading core plugins: " + e.getMessage(), e);
        }
    }

    private Class<?> attemptCorePluginClassLoad(String pluginClassName) {
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return classLoader.loadClass(pluginClassName);
        }
        catch (ClassNotFoundException e) {
            LOG.warn("[GrailsPluginManager] Core plugin [" + pluginClassName +
                    "] not found, resuming load without..");
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return null;
    }

    private void addPlugin(Class<?> plugin) {
        this.foundPluginClasses.add(plugin);
    }

    public void setParentApplicationContext(ApplicationContext parent) {
    }

    private enum PluginParseState {
        PARSING, TYPE, RESOURCE
    }

    class PluginHandler extends DefaultHandler {

        PluginParseState state = PluginParseState.PARSING;

        List<String> pluginTypes = new ArrayList<>();

        List<String> pluginClasses = new ArrayList<>();

        private StringBuilder buff = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals("type")) {
                this.state = PluginParseState.TYPE;
                this.buff = new StringBuilder();
            }
            else if (localName.equals("resource")) {
                this.state = PluginParseState.RESOURCE;
                this.buff = new StringBuilder();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            switch (this.state) {
                case TYPE:
                    this.buff.append(String.valueOf(ch, start, length));
                    break;
                case RESOURCE:
                    this.buff.append(String.valueOf(ch, start, length));
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (this.state) {
                case TYPE:
                    this.pluginTypes.add(this.buff.toString());
                    break;
                case RESOURCE:
                    this.pluginClasses.add(this.buff.toString());
                    break;
            }
            this.state = PluginParseState.PARSING;
        }

    }

}
