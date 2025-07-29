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

    @Override
    public boolean matches(ClassNode classNode) {
        return true;
    }

    @Override
    public void applyDependencies(DependencyCustomizer dependencies) {
        dependencies.add("spring-boot-starter-actuator");
        dependencies.add("spring-boot-starter-logging");
        dependencies.add("spring-boot-starter-validation");
        dependencies.ifAnyMissingClasses("org.springframework.web.servlet.mvc.Controller").add("spring-boot-starter-web");
        dependencies.ifAnyMissingClasses("grails.boot.Grails").add("grace-boot");
        dependencies.ifAnyMissingClasses("grails.core.DefaultGrailsApplication").add("grace-core");
        dependencies.ifAnyMissingClasses("org.grails.plugins.core.CoreGrailsPlugin").add("grace-plugin-core");
        dependencies.ifAnyMissingClasses("org.grails.plugins.codecs.CodecsGrailsPlugin").add("grace-plugin-codecs");
        dependencies.ifAnyMissingClasses("org.grails.plugins.web.controllers.ControllersGrailsPlugin").add("grace-plugin-controllers");
        dependencies.ifAnyMissingClasses("org.grails.plugins.converters.ConvertersGrailsPlugin").add("grace-plugin-converters");
        dependencies.ifAnyMissingClasses("org.grails.plugins.databinding.DataBindingConfiguration").add("grace-plugin-databinding");
        dependencies.ifAnyMissingClasses("org.grails.plugins.domain.DomainClassGrailsPlugin").add("grace-plugin-domain-class");
        dependencies.ifAnyMissingClasses("org.grails.plugins.web.GroovyPagesAutoConfiguration").add("grace-plugin-gsp");
        dependencies.ifAnyMissingClasses("org.grails.plugins.i18n.I18nGrailsPlugin").add("grace-plugin-i18n");
        dependencies.ifAnyMissingClasses("org.grails.plugins.web.interceptors.InterceptorsGrailsPlugin").add("grace-plugin-interceptors");
        dependencies.ifAnyMissingClasses("org.grails.plugins.web.mime.MimeTypesConfiguration").add("grace-plugin-mimetypes");
        dependencies.ifAnyMissingClasses("org.grails.plugins.web.rest.plugin.RestResponderGrailsPlugin").add("grace-plugin-rest");
        dependencies.ifAnyMissingClasses("org.grails.plugins.services.ServicesGrailsPlugin").add("grace-plugin-services");
        dependencies.ifAnyMissingClasses("org.grails.plugins.web.mapping.UrlMappingsGrailsPlugin").add("grace-plugin-url-mappings");
    }

    @Override
    public void applyImports(ImportCustomizer imports) {
        imports.addImports("groovy.transform.CompileStatic",
                "grails.artefact.Artefact",
                "grails.web.Controller",
                "grails.core.GrailsApplication",
                "grails.config.Config");
        imports.addStarImports("grails.boot.annotation");
    }

}
