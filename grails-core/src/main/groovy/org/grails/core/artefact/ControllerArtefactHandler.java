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

import java.lang.annotation.Annotation;

import org.codehaus.groovy.ast.ClassNode;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsControllerClass;

import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.DefaultGrailsControllerClass;

/**
 * Lookup controllers for uris.
 *
 * <p>This class is responsible for looking up controller classes for uris.</p>
 *
 * <p>Lookups are cached in non-development mode, and the cache size can be controlled
 * using the grails.urlmapping.cache.maxsize config property.</p>
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Michael Yan
 * @since 0.5
 */
public class ControllerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Controller";

    public static final String PATH = "controllers";

    public static final String PLUGIN_NAME = "controllers";

    private static final String CONTROLLER_CLASS_NAME = "grails.web.Controller";

    private static Class<?> CONTROLLER_ANNOTATION;

    static {
        ClassLoader classLoader = ControllerArtefactHandler.class.getClassLoader();
        if (ClassUtils.isPresent(CONTROLLER_CLASS_NAME, classLoader)) {
            try {
                CONTROLLER_ANNOTATION = classLoader.loadClass(CONTROLLER_CLASS_NAME);
            }
            catch (ClassNotFoundException ignored) {
            }
        }
    }

    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
                DefaultGrailsControllerClass.CONTROLLER, PATH);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isArtefactClass(ClassNode classNode) {
        if (classNode == null) {
            return false;
        }

        if (CONTROLLER_ANNOTATION != null && GrailsASTUtils.hasAnnotation(classNode, (Class<? extends Annotation>) CONTROLLER_ANNOTATION)) {
            return true;
        }

        return super.isArtefactClass(classNode) && classNode.getAnnotations(new ClassNode(Controller.class)).isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isArtefactClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (CONTROLLER_ANNOTATION != null && clazz.getAnnotation((Class<? extends Annotation>) CONTROLLER_ANNOTATION) != null) {
            return true;
        }

        return super.isArtefactClass(clazz) && clazz.getAnnotation(Controller.class) == null;
    }

}
