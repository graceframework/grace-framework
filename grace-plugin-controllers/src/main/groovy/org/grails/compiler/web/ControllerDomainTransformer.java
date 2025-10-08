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
package org.grails.compiler.web;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;

import grails.artefact.ArtefactTypes;
import grails.compiler.ast.AstTransformer;

import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.grails.plugins.web.controllers.api.ControllersDomainBindingApi;
import org.grails.web.databinding.DefaultASTDatabindingHelper;

/**
 * Adds binding methods to domain classes.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
@AstTransformer
public class ControllerDomainTransformer extends AbstractGrailsArtefactTransformer {

    @Override
    public String getArtefactType() {
        return ArtefactTypes.DOMAIN_CLASS;
    }

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllersDomainBindingApi.class;
    }

    @Override
    protected boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        return false; // don't include instance methods
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // no static methods
    }

    @Override
    protected boolean requiresAutowiring() {
        return false;
    }

    @Override
    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        super.performInjection(source, context, classNode);
        new DefaultASTDatabindingHelper().injectDatabindingCode(source, context, classNode);
    }

    @Override
    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

}
