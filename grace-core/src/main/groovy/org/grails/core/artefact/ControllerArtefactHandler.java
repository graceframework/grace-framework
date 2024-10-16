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
package org.grails.core.artefact;

import org.codehaus.groovy.ast.ClassNode;
import org.springframework.stereotype.Controller;

import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsControllerClass;

import org.grails.core.DefaultGrailsControllerClass;

/**
 * Lookup controllers for uris.
 *
 * <p>This class is responsible for looking up controller classes for uris.</p>
 *
 * <p>Lookups are cached in non-development mode, and the cache size can be controlled using the grails.urlmapping.cache.maxsize config property.</p>
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Michael Yan
 */
public class ControllerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Controller";

    public static final String PLUGIN_NAME = "controllers";

    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
                DefaultGrailsControllerClass.CONTROLLER, false);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isArtefact(ClassNode classNode) {
        boolean isNotSpringController = classNode.getAnnotations(new ClassNode(Controller.class)).isEmpty();
        return isNotSpringController && super.isArtefact(classNode);
    }

    @Override
    public boolean isArtefactClass(Class<?> clazz) {
        boolean isNotSpringController = clazz.getAnnotation(Controller.class) == null;
        return isNotSpringController && super.isArtefactClass(clazz);
    }

}
