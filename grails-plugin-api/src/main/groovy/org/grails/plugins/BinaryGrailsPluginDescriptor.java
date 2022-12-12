/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.List;

import groovy.xml.slurpersupport.GPathResult;
import org.springframework.core.io.Resource;

import org.grails.core.exceptions.GrailsConfigurationException;
import org.grails.io.support.SpringIOUtils;

/**
 * Holds a reference to the parsed grails-plugin.xml descriptor and the
 * resource used to parse the descriptor
 *
 */
public class BinaryGrailsPluginDescriptor {

    private final Resource resource;

    private final List<String> providedClassNames;

    private GPathResult parsedXml;

    public BinaryGrailsPluginDescriptor(Resource resource, List<String> providedClassNames) {
        this.resource = resource;
        this.providedClassNames = providedClassNames;
    }

    /**
     * The resource the descriptor was parsed from
     *
     * @return The resource instance
     */
    public Resource getResource() {
        return this.resource;
    }

    /**
     * @return The class names provided by the plugin
     */
    public List<String> getProvidedClassNames() {
        return this.providedClassNames;
    }

    /**
     * @return The parsed descriptor
     */
    public GPathResult getParsedXml() {
        if (this.parsedXml == null) {
            InputStream inputStream;
            try {
                inputStream = this.resource.getInputStream();
            }
            catch (IOException e) {
                throw new GrailsConfigurationException("Error parsing plugin descriptor: " + this.resource.getFilename(), e);
            }
            try {
                this.parsedXml = SpringIOUtils.createXmlSlurper().parse(inputStream);
            }
            catch (Throwable e) {
                throw new GrailsConfigurationException("Error parsing plugin descriptor: " + this.resource.getFilename(), e);
            }
            finally {
                try {
                    inputStream.close();
                }
                catch (IOException ignored) {
                }
            }
        }
        return this.parsedXml;
    }

}
