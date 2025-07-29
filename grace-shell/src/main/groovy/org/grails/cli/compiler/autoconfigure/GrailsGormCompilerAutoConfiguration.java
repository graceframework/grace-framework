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

import org.grails.cli.compiler.AstUtils;
import org.grails.cli.compiler.CompilerAutoConfiguration;
import org.grails.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for the Grails GORM.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
public class GrailsGormCompilerAutoConfiguration extends CompilerAutoConfiguration {

    @Override
    public boolean matches(ClassNode classNode) {
        return AstUtils.hasAtLeastOneAnnotation(classNode, "Entity", "Service");
    }

    @Override
    public void applyDependencies(DependencyCustomizer dependencies) {
        dependencies.ifAnyMissingClasses("org.grails.plugins.datasource.DataSourceGrailsPlugin").add("grace-plugin-datasource");
        dependencies.ifAnyMissingClasses("org.grails.plugins.domain.DomainClassGrailsPlugin").add("grace-plugin-domain-class");
        dependencies.ifAnyMissingClasses("grails.validation.ConstrainedDelegate").add("grace-plugin-validation");
        dependencies.ifAnyMissingClasses("org.grails.orm.hibernate.HibernateDatastore").add("hibernate", "plugin", "jar", true);
        dependencies.ifAnyMissingClasses("org.h2.Driver").add("h2");
    }

    @Override
    public void applyImports(ImportCustomizer imports) {
        imports.addImports("grails.persistence.Entity");
        imports.addStarImports("grails.gorm.annotation", "grails.gorm.services", "grails.gorm.transactions");
    }

}
