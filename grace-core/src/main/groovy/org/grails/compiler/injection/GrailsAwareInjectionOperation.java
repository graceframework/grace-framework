/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

import grails.artefact.Artefact;
import grails.compiler.ast.ClassInjector;
import grails.core.ArtefactHandler;

import org.grails.core.io.support.GrailsFactoriesLoader;

/**
 * A Groovy compiler injection operation that uses a specified array of
 * ClassInjector instances to attempt AST injection.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.6
 */
public class GrailsAwareInjectionOperation implements CompilationUnit.IPrimaryClassNodeOperation {

    static final ClassNode ARTEFACT_CLASS_NODE = ClassHelper.make(Artefact.class);

    private CompilationUnit compilationUnit;

    private static List<ArtefactHandler> artefactHandlers;

    private static ClassInjector[] classInjectors;

    private static ClassInjector[] globalClassInjectors;

    private ClassInjector[] localClassInjectors;

    private ArtefactHandler[] localArtefactHandlers;

    public GrailsAwareInjectionOperation() {
        initializeState();
    }

    public GrailsAwareInjectionOperation(CompilationUnit compilationUnit) {
        this(compilationUnit, null);
    }

    public GrailsAwareInjectionOperation(ClassInjector[] classInjectors) {
        this(null, classInjectors);
    }

    public GrailsAwareInjectionOperation(CompilationUnit compilationUnit, ClassInjector[] classInjectors) {
        this(compilationUnit, classInjectors, null);
    }

    public GrailsAwareInjectionOperation(CompilationUnit compilationUnit, ClassInjector[] classInjectors, ArtefactHandler[] artefactHandlers) {
        this.compilationUnit = compilationUnit;
        this.localClassInjectors = classInjectors;
        this.localArtefactHandlers = artefactHandlers;
        initializeState();
    }

    public static ClassInjector[] getClassInjectors() {
        if (classInjectors == null) {
            initializeState();
        }
        return classInjectors;
    }

    @Deprecated(forRemoval = true, since = "2024.0.0")
    public static ClassInjector[] getGlobalClassInjectors() {
        if (classInjectors == null) {
            initializeState();
        }
        return globalClassInjectors;
    }

    public ClassInjector[] getLocalClassInjectors() {
        if (this.localClassInjectors == null) {
            return getClassInjectors();
        }
        return this.localClassInjectors;
    }

    /**
     * Get the loaded ArtefactHandlers
     *
     * @return the loaded ArtefactHandlers
     * @since 2024.0.0
     */
    public static ArtefactHandler[] getArtefactHandlers() {
        if (artefactHandlers == null) {
            initializeState();
        }
        return artefactHandlers.toArray(new ArtefactHandler[0]);
    }

    /**
     * Get the loaded local ArtefactHandlers
     *
     * @return the loaded local ArtefactHandlers
     * @since 2024.0.0
     */
    public ArtefactHandler[] getLocalArtefactHandlers() {
        if (this.localArtefactHandlers == null) {
            return getArtefactHandlers();
        }
        return this.localArtefactHandlers;
    }

    @SuppressWarnings("unchecked")
    private static void initializeState() {
        if (artefactHandlers == null) {
            artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler.class);
        }

        if (classInjectors == null) {
            List<ClassInjector> loadedInjectors = GrailsFactoriesLoader.loadFactories(ClassInjector.class);
            loadedInjectors.sort((classInjectorA, classInjectorB) -> {
                if (classInjectorA instanceof Comparable) {
                    return ((Comparable<ClassInjector>) classInjectorA).compareTo(classInjectorB);
                }
                return 0;
            });
            classInjectors = loadedInjectors.toArray(new ClassInjector[0]);
            globalClassInjectors = new ClassInjector[0];
        }
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        for (ArtefactHandler handler : getLocalArtefactHandlers()) {
            if (handler.isArtefact(classNode)) {
                if (classNode.getAnnotations(ARTEFACT_CLASS_NODE).isEmpty()) {
                    AnnotationNode annotationNode = new AnnotationNode(new ClassNode(Artefact.class));
                    annotationNode.addMember("value", new ConstantExpression(handler.getType()));
                    classNode.addAnnotation(annotationNode);

                    ArtefactTypeAstTransformation.performInjection(source, classNode, Arrays.asList(getLocalClassInjectors()), this.compilationUnit);
                    TraitInjectionUtils.processTraitsForNode(source, classNode, handler.getType(), this.compilationUnit);
                }
                break;
            }
        }
    }

}
