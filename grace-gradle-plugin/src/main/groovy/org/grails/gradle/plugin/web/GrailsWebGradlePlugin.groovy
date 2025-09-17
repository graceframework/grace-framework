/*
 * Copyright 2015-2025 the original author or authors.
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
package org.grails.gradle.plugin.web

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

import org.grails.gradle.plugin.core.GrailsGradlePlugin

/**
 * Adds web specific extensions
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GrailsWebGradlePlugin extends GrailsGradlePlugin {

    @Inject
    GrailsWebGradlePlugin(ToolingModelBuilderRegistry registry) {
        super(registry)
    }

    @Override
    void apply(Project project) {
        super.apply(project)

        configureConsoleTask(project)

        configureApplicationCommands(project)

        configureRunScript(project)

        configureRunCommand(project)

        configurePathingJar(project)

        configureProcessResources(project)
    }

    protected configureProcessResources(Project project) {
        Map<String, String> replaceTokens = [
                'info.app.name': project.name,
                'info.app.version': project.version?.toString(),
                'info.app.grailsVersion': grailsVersion
        ]
        SourceSetContainer sourceSets = project.extensions.getByType(JavaPluginExtension).sourceSets
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        project.tasks.named(mainSourceSet.processResourcesTaskName, Copy).configure { Copy copy ->
            copy.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
            copy.from(project.relativePath('src/main/templates')) { CopySpec spec ->
                spec.into('templates')
                spec.include('**/*.gsp')
            }
            copy.from(mainSourceSet.resources) { CopySpec spec ->
                spec.filter(ReplaceTokens, tokens: replaceTokens)
                spec.include('**/*.groovy')
                spec.include('**/*.yml')
                spec.include('**/*.xml')
            }
        }
    }

    protected GrailsProjectType getGrailsProjectType() {
        GrailsProjectType.WEB_APP
    }

}
