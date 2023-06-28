/*
 * Copyright 2022-2023 the original author or authors.
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
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.grails.cli.compiler.CompilerAutoConfiguration;
import org.grails.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for the Grails infrastructure.
 *
 * @author Michael Yan
 * @since 2022.1.0
 */
public class GrailsApplicationCompilerAutoConfiguration extends CompilerAutoConfiguration {

    public static final String[] DEFAULT_IMPORTS = new String[] {
            "grails.boot.annotation",
            "grails.gorm"
    };

    @Override
    public boolean matches(ClassNode classNode) {
        return true;
    }

    @Override
    public void applyDependencies(DependencyCustomizer dependencies) {
        dependencies.add("spring-boot-starter-actuator",
                "spring-boot-starter-logging",
                "spring-boot-starter-validation",
                "spring-boot-starter-web");
        dependencies.add("grails-boot", "grails-core");
        dependencies.add(
                "grails-plugin-core",
                "grails-plugin-codecs",
                "grails-plugin-controllers",
                "grails-plugin-converters",
                "grails-plugin-databinding",
                "grails-plugin-domain-class",
                "grails-plugin-gsp",
                "grails-plugin-i18n",
                "grails-plugin-interceptors",
                "grails-plugin-mimetypes",
                "grails-plugin-rest",
                "grails-plugin-services",
                "grails-plugin-url-mappings");
    }

    @Override
    public void applyImports(ImportCustomizer imports) {
        imports.addImports("groovy.transform.CompileStatic",
                "grails.artefact.Artefact",
                "grails.web.Controller",
                "grails.persistence.Entity",
                "grails.core.GrailsApplication",
                "grails.config.Config");
        imports.addStarImports(DEFAULT_IMPORTS);
    }

}
