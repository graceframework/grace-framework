/*
 * Copyright 2011-2023 the original author or authors.
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

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import grails.compiler.ast.ClassInjector;

/**
 * An AST transform used to apply a named artefact type
 *
 * @author Graeme Rocher
 * @since 2.0
 * @deprecated since 2022.3.0, in favor of {@link ArtefactTypeAstTransformation}
 */
@Deprecated(since = "2022.3.0", forRemoval = true)
@GroovyASTTransformation
public class NamedArtefactTypeAstTransformation extends AbstractArtefactTypeAstTransformation {

    String artefactType;

    public NamedArtefactTypeAstTransformation(String artefactType) {
        this.artefactType = artefactType;
    }

    public void visit(ASTNode[] nodes, SourceUnit source) {
        for (ClassNode node : source.getAST().getClasses()) {
            performInjectionOnArtefactType(source, node, this.artefactType);
        }
    }

    protected void performInjectionOnArtefactType(SourceUnit sourceUnit, ClassNode cNode, String artefactType) {
        try {
            ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors();
            AbstractGrailsArtefactTransformer.addToTransformedClasses(cNode.getName());
            for (ClassInjector injector : classInjectors) {
                if (injector.shouldInject(cNode)) {
                    injector.performInjection(sourceUnit, cNode);
                }
            }
        }
        catch (RuntimeException e) {
            System.err.println("Error occurred calling AST injector [" + getClass() + "]: " + e.getMessage());
            throw e;
        }
    }

}
