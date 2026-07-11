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
package org.grails.gsp.compiler.transform

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import grails.artefact.Artefact
import grails.compiler.traits.TraitInjector
import grails.core.ArtefactHandler

import org.grails.compiler.injection.ArtefactTypeAstTransformation
import org.grails.compiler.injection.GrailsASTUtils
import org.grails.compiler.injection.TraitInjectionUtils
import org.grails.io.support.GrailsFactoriesLoader

/**
 * {@link ASTTransformation} to apply {@link TraitInjector} for GroovyPage.
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class GroovyPageTransform implements ASTTransformation, CompilationUnitAware {

    CompilationUnit compilationUnit

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        List<ClassNode> classes = source.AST.classes

        String sourceName = source.name
        if (!sourceName.endsWith('_gsp.groovy')) {
            return
        }

        List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler)

        for (ClassNode classNode in classes) {
            if (!GrailsASTUtils.isApplied(classNode, this.getClass())) {
                for (ArtefactHandler handler in artefactHandlers) {
                    if (handler.isArtefact(classNode)) {
                        AnnotationNode annotationNode = new AnnotationNode(new ClassNode(Artefact))
                        annotationNode.addMember('value', new ConstantExpression(handler.type))
                        classNode.addAnnotation(annotationNode)

                        ArtefactTypeAstTransformation.performInjectionOnNode(source, classNode, handler.type, compilationUnit)
                        TraitInjectionUtils.processTraitsForNode(source, classNode, handler.type, compilationUnit)
                    }
                }
            }
            GrailsASTUtils.markApplied(classNode, this.getClass())
        }
    }

}
