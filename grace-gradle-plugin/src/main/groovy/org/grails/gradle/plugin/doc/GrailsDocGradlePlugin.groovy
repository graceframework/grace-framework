/*
 * Copyright 2014-2025 the original author or authors.
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
package org.grails.gradle.plugin.doc

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.util.GradleVersion

/**
 * Adds Grails doc publishing support
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GrailsDocGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        verifyGradleVersion()

        Groovydoc groovydocTask = (Groovydoc) project.tasks.findByName('groovydoc')
        Javadoc javadocTask = (Javadoc) project.tasks.findByName('javadoc')

        project.tasks.register('docs', PublishGuideTask).configure { PublishGuideTask docsTask ->
            docsTask.group = 'Documentation'
            docsTask.description = 'Generates documentation for Grace Guides'
            if (project.file("${project.projectDir}/docs").exists()) {
                docsTask.sourceDir = project.file("${project.projectDir}/docs")
            }
            if (project.file("${project.projectDir}/src/docs").exists()) {
                docsTask.sourceDir = project.file("${project.projectDir}/src/docs")
            }
            if (groovydocTask) {
                docsTask.dependsOn(groovydocTask)
            }
            if (javadocTask) {
                docsTask.dependsOn(javadocTask)
            }
        }

        project.tasks.register('docsPdf', PublishPdfTask).configure { PublishPdfTask docsPdfTask ->
            docsPdfTask.dependsOn('docs')
            docsPdfTask.group = 'Documentation'
            docsPdfTask.description = 'Generates PDF documentation for Grace Guides'
        }
    }

    private void verifyGradleVersion() {
        GradleVersion currentVersion = GradleVersion.current()
        if (currentVersion < GradleVersion.version('8.0')) {
            throw new GradleException('Grace plugin requires Gradle 8.x (8.0 or later). '
                    + 'The current version is ' + currentVersion)
        }
    }

}
