/*
 * Copyright 2004-2026 the original author or authors.
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

import java.io.File;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;
import org.springframework.core.Ordered;

import grails.artefact.ArtefactTypes;
import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsClass;
import grails.core.GrailsDomainClass;
import grails.util.GrailsStringUtils;

import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.DefaultGrailsDomainClass;
import org.grails.datastore.mapping.model.MappingContext;

/**
 * Evaluates the conventions that define a domain class in Grails.
 *
 * @author Graeme Rocher
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Michael Yan
 * @since 0.5
 */
public class DomainClassArtefactHandler extends ArtefactHandlerAdapter implements Ordered {

    public static final String TYPE = ArtefactTypes.DOMAIN_CLASS;

    public static final String PATH = "domain";

    public static final String PLUGIN_NAME = "domainClass";

    public DomainClassArtefactHandler() {
        super(TYPE, GrailsDomainClass.class, DefaultGrailsDomainClass.class, null, PATH, true);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public GrailsClass newArtefactClass(Class<?> artefactClass) {
        return new DefaultGrailsDomainClass(artefactClass);
    }

    public GrailsClass newArtefactClass(Class<?> artefactClass, MappingContext mappingContext) {
        return new DefaultGrailsDomainClass(artefactClass, mappingContext);
    }

    @Override
    protected boolean isValidArtefactClassNode(ClassNode classNode, int modifiers) {
        return !classNode.isEnum() && !(classNode instanceof InnerClassNode);
    }

    @Override
    public boolean isArtefactClass(ClassNode classNode) {
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
        String projectDir = ast.getNodeMetaData(GrailsASTUtils.META_DATA_KEY_PROJECT_DIR);
        String grailsAppDir = ast.getNodeMetaData(GrailsASTUtils.META_DATA_KEY_GRAILS_APP_DIR);

        if (filename == null || projectDir == null) {
            return false;
        }
        boolean inGrailsAppDir = GrailsStringUtils.isNotBlank(grailsAppDir)
                && filename.startsWith(grailsAppDir + File.separatorChar + PATH) && filename.endsWith(".groovy");

        return !GrailsASTUtils.isJpaEntityClass(classNode) && inGrailsAppDir;
    }

    @Override
    public boolean isArtefactClass(Class<?> clazz) {
        return GrailsASTUtils.isDomainClass(clazz);
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
