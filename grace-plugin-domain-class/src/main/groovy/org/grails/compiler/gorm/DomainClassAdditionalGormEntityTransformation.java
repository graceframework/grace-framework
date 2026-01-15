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
package org.grails.compiler.gorm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

import grails.artefact.Artefact;
import grails.artefact.ArtefactTypes;
import grails.compiler.ast.ClassInjector;
import grails.compiler.traits.TraitInjector;

import org.grails.compiler.injection.ArtefactTypeAstTransformation;
import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.grails.compiler.injection.TraitInjectionUtils;

/**
 * {@link AdditionalGormEntityTransformation} for GORM Entity exists in Grails,
 * it will add {@link Artefact} annotation for Domain Class,
 * then perform to inject {@link ClassInjector} and {@link TraitInjector}.
 *
 * @author Michael Yan
 * @since 3.0
 */
public class DomainClassAdditionalGormEntityTransformation implements AdditionalGormEntityTransformation {

    private CompilationUnit compilationUnit;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void visit(ClassNode classNode, SourceUnit sourceUnit) {
        if (GrailsASTUtils.hasAnnotation(classNode, Artefact.class)) {
            return;
        }
        try {
            GrailsASTUtils.addAnnotationOrGetExisting(classNode, Artefact.class, Map.of("value", "Domain"));
        }
        catch (Throwable ignored) {
        }

        List<ClassInjector> injectors = Arrays.stream(GrailsAwareInjectionOperation.getClassInjectors())
                .filter((classInjector -> !(classInjector.getClass().equals(GormTransformer.class)))).toList();

        ArtefactTypeAstTransformation.performInjection(sourceUnit, classNode, injectors, this.compilationUnit);
        TraitInjectionUtils.processTraitsForNode(sourceUnit, classNode, ArtefactTypes.DOMAIN_CLASS, this.compilationUnit);
    }

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
    }

    @Override
    public void setCompilationUnit(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

}
