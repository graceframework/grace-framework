/*
 * Copyright 2021-2023 the original author or authors.
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

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;

import grails.artefact.Artefact;
import grails.core.ArtefactHandler;
import grails.core.ArtefactInfo;
import grails.core.GrailsClass;

/**
 * An {@link grails.core.ArtefactHandler} that identifies the Grails Plugin class
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsPluginArtefactHandler implements ArtefactHandler {

    public static final String TYPE = "GrailsPlugin";
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
        SourceUnit source = classNode.getModule().getContext();
        String filename = source.getName();
        ModuleNode ast = source.getAST();
        String projectDir = ast.getNodeMetaData("PROJECT_DIR");
        String grailsAppDir = ast.getNodeMetaData("GRAILS_APP_DIR");
        if (filename == null || projectDir == null || grailsAppDir == null) {
            return false;
        }

        if (classNode.isEnum() || classNode.isInterface() || (classNode instanceof InnerClassNode)) {
            return false;
        }

        return (filename.startsWith(grailsAppDir + File.separatorChar + "plugins")
                    || filename.startsWith(projectDir + File.separatorChar + "src" + File.separatorChar + "main"))
                && filename.endsWith("GrailsPlugin.groovy");
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

        boolean ok = clazz.getName().endsWith(TYPE) && !Closure.class.isAssignableFrom(clazz);
        if (ok) {
            ok = !Modifier.isAbstract(clazz.getModifiers());
        }

        return ok;
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

}
