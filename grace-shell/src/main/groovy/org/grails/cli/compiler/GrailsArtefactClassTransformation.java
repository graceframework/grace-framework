/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.cli.compiler;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import groovy.lang.GroovyClassLoader;
import groovy.transform.CompilationUnitAware;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import grails.artefact.Artefact;
import grails.compiler.ast.ClassInjector;
import grails.compiler.traits.TraitInjector;
import grails.core.ArtefactHandler;
import grails.core.ArtefactInfo;
import grails.core.GrailsClass;
import org.grails.compiler.injection.ArtefactTypeAstTransformation;
import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.compiler.injection.TraitInjectionUtils;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.core.io.support.GrailsFactoriesLoader;

/**
 * {@link ASTTransformation} for applying Grails' transformations to the Grails Artefacts.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GrailsArtefactClassTransformation implements ASTTransformation, CompilationUnitAware {

    private static final ClassNode ARTEFACT_CLASS_NODE = ClassHelper.make(Artefact.class);

    private final GroovyClassLoader loader;
    private CompilationUnit compilationUnit;

    public GrailsArtefactClassTransformation(GroovyClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public void setCompilationUnit(final CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        URLClassLoader urlClassLoader = new URLClassLoader(this.loader.getURLs(), Thread.currentThread().getContextClassLoader());
        List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler.class, urlClassLoader);
        List<ClassInjector> classInjectors = GrailsFactoriesLoader.loadFactories(ClassInjector.class, urlClassLoader);
        List<TraitInjector> traitInjectors = GrailsFactoriesLoader.loadFactories(TraitInjector.class, urlClassLoader);

        List<ArtefactHandler> delegatedArtefactHandlers = artefactHandlers
                .stream().map(DelegatedArtefactHandler::new).collect(Collectors.toList());

        ModuleNode ast = source.getAST();
        List<ClassNode> classNodes = new ArrayList<>(ast.getClasses());
        for (ClassNode classNode : classNodes) {
            for (ArtefactHandler handler : delegatedArtefactHandlers) {
                if (handler.isArtefact(classNode)) {
                    if (classNode.getAnnotations(ARTEFACT_CLASS_NODE).isEmpty()) {
                        AnnotationNode annotationNode = new AnnotationNode(new ClassNode(Artefact.class));
                        annotationNode.addMember("value", new ConstantExpression(handler.getType()));
                        classNode.addAnnotation(annotationNode);

                        ArtefactTypeAstTransformation.performInjection(source, classNode, classInjectors, this.compilationUnit);
                        TraitInjectionUtils.processTraitsForNode(source, classNode, handler.getType(), traitInjectors, this.compilationUnit);
                    }
                }
            }
        }
    }

    private static class DelegatedArtefactHandler implements ArtefactHandler {

        private final ArtefactHandler delegate;

        DelegatedArtefactHandler(ArtefactHandler artefactHandler) {
            this.delegate = artefactHandler;
        }

        @Override
        public String getPluginName() {
            return this.delegate.getPluginName();
        }

        @Override
        public String getType() {
            return this.delegate.getType();
        }

        @Override
        public boolean isArtefact(ClassNode classNode) {
            if (classNode.isEnum() || classNode.isInterface() || (classNode instanceof InnerClassNode)
                    || classNode.isAbstract()) {
                return false;
            }
            if (getType().equals(DomainClassArtefactHandler.TYPE)) {
                return GrailsASTUtils.hasAnnotation(classNode, Artefact.class);
            }
            String name = classNode.getName();
            return name.endsWith(getType());
        }

        @Override
        public boolean isArtefact(Class<?> aClass) {
            return true;
        }

        @Override
        public GrailsClass newArtefactClass(Class<?> artefactClass) {
            return null;
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
            return false;
        }
    }

}
