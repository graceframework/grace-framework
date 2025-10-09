/*
 * Copyright 2004-2025 the original author or authors.
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
package org.grails.core.artefact;

import org.codehaus.groovy.ast.ClassNode;
import org.springframework.stereotype.Service;

import grails.artefact.ArtefactTypes;
import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsServiceClass;

import org.grails.core.DefaultGrailsServiceClass;

/**
 * Handler for Service Artefact
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Michael Yan
 * @since 0.5
 */
public class ServiceArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = ArtefactTypes.SERVICE;

    public static final String PATH = "services";

    public static final String PLUGIN_NAME = "services";

    public ServiceArtefactHandler() {
        super(TYPE, GrailsServiceClass.class, DefaultGrailsServiceClass.class,
                DefaultGrailsServiceClass.SERVICE, PATH);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isArtefact(ClassNode classNode) {
        return super.isArtefact(classNode) && classNode.getAnnotations(new ClassNode(Service.class)).isEmpty();
    }

    @Override
    public boolean isArtefactClass(Class<?> clazz) {
        return super.isArtefactClass(clazz) && clazz.getAnnotation(Service.class) == null;
    }

}
