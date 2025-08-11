/*
 * Copyright 2014-2025 the original author or authors.
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
package org.grails.core.artefact

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit

import grails.core.ArtefactHandlerAdapter
import grails.core.DefaultGrailsClass
import grails.core.GrailsClass

import org.grails.compiler.injection.GrailsASTUtils

/**
 * An {@link grails.core.ArtefactHandler} that identifies the Application class
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class ApplicationArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = 'Application'
    public static final String PATH = 'boot'

    ApplicationArtefactHandler() {
        super(TYPE, GrailsClass, DefaultGrailsClass, TYPE, PATH)
    }

    @Override
    boolean isArtefact(ClassNode classNode) {
        if (classNode == null || classNode.isEnum() || classNode.isInterface() || (classNode instanceof InnerClassNode) || classNode.isAbstract()) {
            return false
        }

        if (!isArtefactClass(classNode)) {
            return false
        }

        String className = classNode.getName()
        return className.endsWith(TYPE)
    }

    @Override
    boolean isArtefactClass(ClassNode classNode) {
        if (classNode == null) {
            return false
        }

        if (hasArtefactAnnotation(classNode, TYPE)) {
            return true
        }
        if (classNode.getModule() == null || classNode.getModule().getContext() == null) {
            return false
        }

        SourceUnit source = classNode.getModule().getContext()
        String filename = source.getName()
        ModuleNode ast = source.getAST()
        String projectDir = ast.getNodeMetaData(GrailsASTUtils.META_DATA_KEY_PROJECT_DIR)
        String grailsAppDir = ast.getNodeMetaData(GrailsASTUtils.META_DATA_KEY_GRAILS_APP_DIR)

        if (filename == null || projectDir == null || grailsAppDir == null) {
            return false
        }

        return (filename.startsWith(grailsAppDir + File.separatorChar + PATH) ||
                filename.startsWith(projectDir + File.separatorChar + "src")) && filename.endsWith(TYPE + ".groovy")
    }

}
