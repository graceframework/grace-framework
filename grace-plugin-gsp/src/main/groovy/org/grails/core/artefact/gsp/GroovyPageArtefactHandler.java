/*
 * Copyright 2022-2026 the original author or authors.
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

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;

import grails.artefact.ArtefactTypes;
import grails.core.ArtefactHandler;
import grails.core.ArtefactInfo;
import grails.core.GrailsClass;

import org.grails.core.gsp.DefaultGroovyPageClass;
import org.grails.gsp.GroovyPage;

/**
 * Handler for Groovy Page Artefact
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
public class GroovyPageArtefactHandler implements ArtefactHandler {

    public static final String TYPE = ArtefactTypes.GROOVY_PAGE;
    public static final String PLUGIN_NAME = "GroovyPages";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isArtefact(ClassNode classNode) {
        return classNode.isDerivedFrom(ClassHelper.make(GroovyPage.class));
    }

    @Override
    public boolean isArtefact(Class<?> clazz) {
        return clazz.isAssignableFrom(GroovyPage.class);
    }

    @Override
    public GrailsClass newArtefactClass(Class<?> artefactClass) {
        return new DefaultGroovyPageClass(artefactClass);
    }

    @Override
    public void initialize(ArtefactInfo artefacts) {
    }

    @Override
    public GrailsClass getArtefactForFeature(Object feature) {
        return null;
    }

    @Override
    public boolean isArtefactGrailsClass(GrailsClass artefactGrailsClass) {
        return DefaultGroovyPageClass.class.isAssignableFrom(artefactGrailsClass.getClass());
    }

}
