/*
 * Copyright 2006-2023 the original author or authors.
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

import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

import grails.compiler.ast.ClassInjector;

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

    private static ClassInjector[] classInjectors;

    private static ClassInjector[] globalClassInjectors;

    private ClassInjector[] localClassInjectors;

    public GrailsAwareInjectionOperation() {
        initializeState();
    }

    public GrailsAwareInjectionOperation(ClassInjector[] classInjectors) {
        this();
        this.localClassInjectors = classInjectors;
    }

    public static ClassInjector[] getClassInjectors() {
        if (classInjectors == null) {
            initializeState();
        }
        return classInjectors;
    }

    @Deprecated(forRemoval = true, since = "2023.0.0")
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

    @SuppressWarnings("unchecked")
    private static void initializeState() {
        if (classInjectors != null) {
            return;
        }

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

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        ClassInjector[] classInjectors = getLocalClassInjectors();
        if (classInjectors == null || classInjectors.length == 0) {
            classInjectors = getClassInjectors();
        }
        for (ClassInjector classInjector : classInjectors) {
            if (classInjector.shouldInject(classNode)) {
                classInjector.performInjection(source, context, classNode);
            }
        }
    }

}
