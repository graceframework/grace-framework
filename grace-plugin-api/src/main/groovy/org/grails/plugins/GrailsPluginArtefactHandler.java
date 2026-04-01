/*
 * Copyright 2021-2026 the original author or authors.
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

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.SourceUnit;

import grails.artefact.Artefact;
import grails.core.ArtefactHandler;
import grails.core.ArtefactInfo;
import grails.core.GrailsClass;
import grails.util.GrailsStringUtils;

/**
 * An {@link grails.core.ArtefactHandler} that identifies the Grails Plugin class
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsPluginArtefactHandler implements ArtefactHandler {

    public static final String TYPE = "GrailsPlugin";

    public static final String META_DATA_KEY_GRAILS_APP_DIR = "GRAILS_APP_DIR";

    public static final String META_DATA_KEY_PROJECT_DIR = "PROJECT_DIR";

    private static final GrailsPluginArtefactHandler INSTANCE = new GrailsPluginArtefactHandler();

    public GrailsPluginArtefactHandler() {

    }

    @Override
    public String getPluginName() {
        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isArtefact(ClassNode classNode) {
        if (classNode == null || classNode.isEnum() || classNode.isInterface() || (classNode instanceof InnerClassNode) || classNode.isAbstract()) {
            return false;
        }

        if (!isArtefactClass(classNode)) {
            return false;
        }

        String className = classNode.getName();
        return className.endsWith(TYPE);
    }

    @Override
    public boolean isArtefact(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        Artefact annotation = clazz.getAnnotation(Artefact.class);
        if (annotation == null || !annotation.value().equals(TYPE)) {
            return false;
        }

        return  clazz.getName().endsWith(TYPE)
                && !Closure.class.isAssignableFrom(clazz)
                && !Modifier.isAbstract(clazz.getModifiers());
    }

    @Override
    public GrailsClass newArtefactClass(Class<?> artefactClass) {
        return new GrailsPluginClass(artefactClass);
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
        return GrailsPluginClass.class.isAssignableFrom(artefactGrailsClass.getClass());
    }

    public static boolean isGrailsPlugin(Class<?> clazz) {
        return INSTANCE.isArtefact(clazz);
    }

    private boolean isArtefactClass(ClassNode classNode) {
        if (classNode == null) {
            return false;
        }

        if (hasArtefactAnnotation(classNode, TYPE)) {
            return true;
        }
        if (classNode.getModule() == null || classNode.getModule().getContext() == null) {
            return false;
        }

        SourceUnit source = classNode.getModule().getContext();
        String filename = source.getName();
        ModuleNode ast = source.getAST();
        String projectDir = ast.getNodeMetaData(META_DATA_KEY_PROJECT_DIR);
        String grailsAppDir = ast.getNodeMetaData(META_DATA_KEY_GRAILS_APP_DIR);
        if (filename == null || projectDir == null) {
            return false;
        }
        boolean inGrailsAppDir = GrailsStringUtils.isNotBlank(grailsAppDir)
                && filename.startsWith(grailsAppDir + File.separatorChar + "plugins") && filename.endsWith(TYPE + ".groovy");
        boolean inProjectDir = filename.startsWith(projectDir + File.separatorChar + "src")
                && filename.endsWith(TYPE + ".groovy");
        return inProjectDir || inGrailsAppDir;
    }

    private boolean hasArtefactAnnotation(ClassNode classNode, String value) {
        List<AnnotationNode> annotationNodes = classNode.getAnnotations(new ClassNode(Artefact.class));

        for (AnnotationNode node : annotationNodes) {
            Expression artefactValue = node.getMember("value");
            if (artefactValue instanceof ConstantExpression) {
                Object artefactType = ((ConstantExpression) artefactValue).getValue();
                if (artefactType != null && artefactType.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

}
