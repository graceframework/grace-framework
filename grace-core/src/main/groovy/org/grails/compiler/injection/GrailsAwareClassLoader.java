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

import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;

import grails.compiler.ast.ClassInjector;

/**
 * A class loader that is aware of Groovy sources and injection operations.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.6
 */
public class GrailsAwareClassLoader extends GroovyClassLoader {

    private final CompilerConfiguration config;

    private CompilationUnit compilationUnit;

    private Map<?, ?> metaDataMap = new HashMap<>();

    private ClassInjector[] classInjectors;

    private boolean disabledGrailsAwareInjectionOperation = false;

    public GrailsAwareClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public GrailsAwareClassLoader(ClassLoader loader) {
        this(loader, new CompilerConfiguration());
    }

    public GrailsAwareClassLoader(ClassLoader loader, CompilerConfiguration config) {
        this(loader, config, false);
    }

    public GrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration config, boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath);
        this.config = config;
    }

    public void setMetaDataMap(Map<?, ?> metaDataMap) {
        this.metaDataMap = metaDataMap;
    }

    public Map<?, ?> getMetaDataMap() {
        if (this.metaDataMap == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(this.metaDataMap);
    }

    public void setClassInjectors(ClassInjector[] classInjectors) {
        this.classInjectors = classInjectors;
    }

    /**
     * Disable the Global ASTTransformations
     *
     * @param disabledGlobalASTTransformations whether to disable the global ASTTransformations
     * @since 2024.0.0
     */
    public void setDisabledGlobalASTTransformations(boolean disabledGlobalASTTransformations) {
        if (disabledGlobalASTTransformations) {
            Set<String> globalASTTransformations = new HashSet<>();
            globalASTTransformations.add(GlobalGrailsClassInjectorTransformation.class.getName());
            globalASTTransformations.add(GlobalGrailsPluginTransformation.class.getName());
            this.config.setDisabledGlobalASTTransformations(globalASTTransformations);
        }
    }

    /**
     * Disable the Grails AwareInjectionOperation
     *
     * @param disabledGrailsAwareInjectionOperation whether to disable the Grails AwareInjectionOperation
     * @since 2024.0.0
     */
    public void setDisabledGrailsAwareInjectionOperation(boolean disabledGrailsAwareInjectionOperation) {
        this.disabledGrailsAwareInjectionOperation = disabledGrailsAwareInjectionOperation;
    }

    /**
     * Get the ClassNode by the name
     *
     * @param name the name of the ClassNode
     * @since 2024.0.0
     */
    public ClassNode getClassNode(String name) {
        return this.compilationUnit.getClassNode(name);
    }

    /**
     * @see groovy.lang.GroovyClassLoader#createCompilationUnit(org.codehaus.groovy.control.CompilerConfiguration, java.security.CodeSource)
     */
    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit cu = super.createCompilationUnit(config, source);

        cu.addPhaseOperation((sourceUnit, context, classNode) -> sourceUnit.getAST().setMetaDataMap(getMetaDataMap()), Phases.SEMANTIC_ANALYSIS);

        if (!this.disabledGrailsAwareInjectionOperation) {
            cu.addPhaseOperation(getGrailsAwareInjectionOperation(cu), Phases.CANONICALIZATION);
        }

        this.compilationUnit = cu;

        return cu;
    }

    protected GrailsAwareInjectionOperation getGrailsAwareInjectionOperation(CompilationUnit cu) {
        GrailsAwareInjectionOperation operation;

        if (this.classInjectors == null) {
            operation = new GrailsAwareInjectionOperation(cu);
        }
        else {
            operation = new GrailsAwareInjectionOperation(cu, this.classInjectors);
        }
        return operation;
    }

}
