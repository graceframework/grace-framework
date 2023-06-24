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
package org.grails.compiler.injection;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import grails.persistence.Entity;

import org.grails.core.artefact.DomainClassArtefactHandler;

/**
 * Injects the necessary fields and behaviors into a domain class in order to make it a property GORM entity.
 *
 * @author Graeme Rocher
 * @since 1.1
 * @see grails.persistence.Entity
 */
@GroovyASTTransformation
public class EntityASTTransformation extends ArtefactTypeAstTransformation {

    @Override
    protected String resolveArtefactType(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode) {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    protected Class<?> getAnnotationTypeClass() {
        return Entity.class;
    }

}
