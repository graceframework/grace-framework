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
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer
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

        TaskContainer tasks = project.tasks
        tasks.register('docs', PublishGuideTask) { PublishGuideTask docsTask ->
            docsTask.notCompatibleWithConfigurationCache('DocPublisher use Ant tasks')
            docsTask.group = 'Documentation'
            docsTask.description = 'Generates documentation for Grace Guides'
            docsTask.getTargetDir().set(project.layout.buildDirectory.dir('docs/manual'))
            docsTask.getWorkDir().set(project.layout.buildDirectory.dir('tmp'))
            docsTask.getResourcesDir().set(docsTask.getSourceDir().dir('resources'))

            if (project.file('docs').exists()) {
                docsTask.getSourceDir().set(project.file('docs'))
            }
            if (project.file('src/docs').exists()) {
                docsTask.getSourceDir().set(project.file('src/docs'))
            }

            if (tasks.names.contains(GroovyPlugin.GROOVYDOC_TASK_NAME)) {
                docsTask.dependsOn(tasks.named(GroovyPlugin.GROOVYDOC_TASK_NAME, Groovydoc))
            }
            if (tasks.names.contains(JavaPlugin.JAVADOC_TASK_NAME)) {
                docsTask.dependsOn(tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc))
            }
        }

        tasks.register('docsPdf', PublishPdfTask).configure { PublishPdfTask docsPdfTask ->
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
