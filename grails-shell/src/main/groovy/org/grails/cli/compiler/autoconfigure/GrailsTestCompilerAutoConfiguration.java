/*
 * Copyright 2014-2023 the original author or authors.
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
package org.grails.cli.compiler.autoconfigure;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.grails.cli.compiler.AstUtils;
import org.grails.cli.compiler.CompilerAutoConfiguration;
import org.grails.cli.compiler.DependencyCustomizer;

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
public class GrailsTestCompilerAutoConfiguration extends CompilerAutoConfiguration {

    public static final String[] DEFAULT_IMPORTS = new String[] {
            "spock.lang",
            "grails.test.mixin",
            "grails.test.mixin.integration",
            "grails.test.mixin.support",
            "grails.artefact"
    };

    @Override
    public boolean matches(ClassNode classNode) {
        return AstUtils.subclasses(classNode, "Specification");
    }

    @Override
    public void applyDependencies(DependencyCustomizer dependencies) throws CompilationFailedException {
        dependencies.add("org.grails", "grails-plugin-testing");
    }

    @Override
    public void applyImports(ImportCustomizer imports) throws CompilationFailedException {
        imports.addStarImports(DEFAULT_IMPORTS);
    }

}
