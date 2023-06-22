/*
 * Copyright 2004-2023 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import grails.core.ArtefactHandlerAdapter;
import grails.core.ArtefactInfo;
import grails.core.GrailsClass;
import grails.core.gsp.GrailsTagLibClass;

import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.gsp.DefaultGrailsTagLibClass;

/**
 * Configures tag libraries within namespaces in Grails.
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 * @author a.shneyderman
 * @author Michael Yan
 *
 * @since 3.3
 */
public class TagLibArtefactHandler extends ArtefactHandlerAdapter {

    public static final String PLUGIN_NAME = "groovyPages";

    public static final String TYPE = "TagLib";

    public static final String PATH = "taglib";

    private static final String TAGLIB_CLASS_NAME = "grails.gsp.TagLib";

    private Map<String, GrailsTagLibClass> tag2libMap = new HashMap<>();

    private final Map<String, GrailsTagLibClass> namespace2tagLibMap = new HashMap<>();

    private static Class<?> TAGLIB_ANNOTATION;

    static {
        ClassLoader classLoader = TagLibArtefactHandler.class.getClassLoader();
        if (ClassUtils.isPresent(TAGLIB_CLASS_NAME, classLoader)) {
            try {
                TAGLIB_ANNOTATION = classLoader.loadClass(TAGLIB_CLASS_NAME);
            }
            catch (ClassNotFoundException ignored) {
            }
        }
    }

    public TagLibArtefactHandler() {
        super(TYPE, GrailsTagLibClass.class, DefaultGrailsTagLibClass.class, TYPE, PATH);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean isArtefactClass(ClassNode classNode) {
        if (classNode == null) {
            return false;
        }

        if (TAGLIB_ANNOTATION != null && GrailsASTUtils.hasAnnotation(classNode, (Class<? extends Annotation>) TAGLIB_ANNOTATION)) {
            return true;
        }

        return super.isArtefactClass(classNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isArtefactClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (TAGLIB_ANNOTATION != null && clazz.getAnnotation((Class<? extends Annotation>) TAGLIB_ANNOTATION) != null) {
            return true;
        }

        return hasArtefactAnnotation(clazz, this.type);
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
