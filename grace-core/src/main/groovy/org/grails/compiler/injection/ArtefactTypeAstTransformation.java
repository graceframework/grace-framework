/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import groovy.transform.CompilationUnitAware;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.springframework.core.OrderComparator;
import org.springframework.util.ClassUtils;

import grails.artefact.Artefact;
import grails.build.logging.GrailsConsole;
import grails.compiler.ast.ClassInjector;

/**
 * A transformation used to apply transformers to classes not located in Grails
 * directory structure. For example any class can be annotated with
 * &#064;Artefact("Controller") to make it into a controller no matter what the location.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
@GroovyASTTransformation
public class ArtefactTypeAstTransformation extends AbstractArtefactTypeAstTransformation implements CompilationUnitAware {

    private static final ClassNode MY_TYPE = new ClassNode(Artefact.class);

    protected CompilationUnit compilationUnit;

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];

        if (!isArtefactAnnotationNode(node) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode cNode = (ClassNode) parent;
        if (cNode.isInterface()) {
            throw new RuntimeException("Error processing interface '" + cNode.getName() + "'. @" +
                    getAnnotationType().getNameWithoutPackage() + " not allowed for interfaces.");
        }

        if (isApplied(cNode)) {
            return;
        }

        String artefactType = resolveArtefactType(sourceUnit, node, cNode);
        if (artefactType != null) {
            AbstractGrailsArtefactTransformer.addToTransformedClasses(cNode.getName());

            addArtefactAnnotation(sourceUnit, node, cNode, artefactType);
        }

        performInjectionOnArtefactType(sourceUnit, cNode, artefactType);

        performTraitInjectionOnArtefactType(sourceUnit, cNode, artefactType);

        markApplied(cNode);
    }

    protected void performInjectionOnArtefactType(SourceUnit sourceUnit, ClassNode cNode, String artefactType) {
        performInjectionOnNode(sourceUnit, cNode, artefactType, this.compilationUnit);
    }

    public static void performInjectionOnNode(SourceUnit sourceUnit, ClassNode cNode, String artefactType, CompilationUnit compilationUnit) {
        List<ClassInjector> injectors = Arrays.asList(GrailsAwareInjectionOperation.getClassInjectors());
        performInjection(sourceUnit, cNode, injectors, compilationUnit);
    }

    public static void performInjection(SourceUnit sourceUnit, ClassNode cNode, Collection<ClassInjector> injectors,
            CompilationUnit compilationUnit) {
        List<ClassInjector> classInjectors = new ArrayList<>(injectors);
        OrderComparator.sort(classInjectors);
        try {
            for (ClassInjector injector : classInjectors) {
                if (injector instanceof CompilationUnitAware) {
                    ((CompilationUnitAware) injector).setCompilationUnit(compilationUnit);
                }
                if (!GrailsASTUtils.isApplied(cNode, injector.getClass()) && injector.shouldInject(cNode)) {
                    injector.performInjection(sourceUnit, cNode);
                    GrailsASTUtils.markApplied(cNode, injector.getClass());
                }
            }
        }
        catch (RuntimeException e) {
            if (ClassUtils.isPresent("jline.console.completer.CompletionHandler", ArtefactTypeAstTransformation.class.getClassLoader())) {
                GrailsConsole.getInstance().error("Error occurred calling AST injector: " + e.getMessage(), e);
            }
            else {
                System.err.println("Error occurred calling AST injector: " + e.getMessage());
            }
            throw e;
        }
    }

    protected void performTraitInjectionOnArtefactType(SourceUnit sourceUnit,
            ClassNode cNode, String artefactType) {
        TraitInjectionUtils.processTraitsForNode(sourceUnit, cNode, artefactType, this.compilationUnit);
    }

    protected void addArtefactAnnotation(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode, String artefactType) {
        if (!MY_TYPE.equals(annotationNode.getClassNode()) && classNode.getAnnotations(MY_TYPE).isEmpty()) {
            // add @Artefact annotation to resulting class so that "shortcut" annotations like @TagLib
            // also produce an @Artefact annotation in the resulting class file
            AnnotationNode annotation = new AnnotationNode(MY_TYPE);
            annotation.addMember("value", new ConstantExpression(artefactType));
            classNode.addAnnotation(annotation);
        }
    }

    protected boolean isArtefactAnnotationNode(AnnotationNode annotationNode) {
        return getAnnotationType().equals(annotationNode.getClassNode());
    }

    protected ClassNode getAnnotationType() {
        return new ClassNode(getAnnotationTypeClass());
    }

    protected Class<?> getAnnotationTypeClass() {
        return MY_TYPE.getTypeClass();
    }

    @Override
    public void setCompilationUnit(CompilationUnit unit) {
        this.compilationUnit = unit;
    }

}
