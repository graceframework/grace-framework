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
package org.grails.core.artefact.gsp;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import grails.core.ArtefactHandlerAdapter;
import grails.core.ArtefactInfo;
import grails.core.GrailsClass;
import grails.core.gsp.GrailsTagLibClass;

import org.grails.core.gsp.DefaultGrailsTagLibClass;

/**
 * Configures tag libraries within namespaces in Grails.
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 * @author a.shneyderman
 *
 * @since 3.3
 */
public class TagLibArtefactHandler extends ArtefactHandlerAdapter {

    public static final String PLUGIN_NAME = "groovyPages";

    public static final String TYPE = "TagLib";

    private Map<String, GrailsTagLibClass> tag2libMap = new HashMap<>();

    private Map<String, GrailsTagLibClass> namespace2tagLibMap = new HashMap<>();

    public TagLibArtefactHandler() {
        super(TYPE, GrailsTagLibClass.class, DefaultGrailsTagLibClass.class, TYPE);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Creates a map of tags (keyed on "${namespace}:${tagName}") to tag libraries.
     */
    @Override
    public void initialize(ArtefactInfo artefacts) {
        this.tag2libMap = new HashMap<>();
        for (GrailsClass aClass : artefacts.getGrailsClasses()) {
            GrailsTagLibClass taglibClass = (GrailsTagLibClass) aClass;
            String namespace = taglibClass.getNamespace();
            this.namespace2tagLibMap.put(namespace, taglibClass);
            for (Object o : taglibClass.getTagNames()) {
                String tagName = namespace + ":" + o;
                if (!this.tag2libMap.containsKey(tagName)) {
                    this.tag2libMap.put(tagName, taglibClass);
                }
                else {
                    GrailsTagLibClass current = this.tag2libMap.get(tagName);
                    if (!taglibClass.equals(current)) {
                        LoggerFactory.getLogger(TagLibArtefactHandler.class).info("There are conflicting tags: " + taglibClass.getFullName() + "." +
                                tagName + " vs. " + current.getFullName() + "." + tagName +
                                ". The former will take precedence.");
                        this.tag2libMap.put(tagName, taglibClass);
                    }
                }
            }
        }
    }

    /**
     * Looks up a tag library by using either a full qualified tag name such as g:link or
     * via namespace such as "g".
     *
     * @param feature The tag name or namespace
     * @return A GrailsClass instance representing the tag library
     */
    @Override
    public GrailsClass getArtefactForFeature(Object feature) {
        final Object tagLib = this.tag2libMap.get(feature);
        if (tagLib != null) {
            return (GrailsClass) tagLib;
        }

        return this.namespace2tagLibMap.get(feature);
    }

}
