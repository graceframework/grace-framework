/*
 * Copyright 2015-2026 the original author or authors.
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
package org.grails.gradle.plugin.web.views

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War

import grails.util.GrailsNameUtils

import org.grails.gradle.plugin.core.GrailsExtension
import org.grails.gradle.plugin.core.GrailsGradlePlugin
import org.grails.gradle.plugin.util.SourceSets

/**
 * Abstract implementation of a plugin that compiles views
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
abstract class AbstractGroovyTemplatePlugin implements Plugin<Project> {

    final Class<? extends AbstractGroovyTemplateCompileTask> taskClass
    final String taskName
    final String fileExtension

    AbstractGroovyTemplatePlugin(Class<? extends AbstractGroovyTemplateCompileTask> taskClass, String fileExtension) {
        this.taskClass = taskClass
        this.fileExtension = fileExtension
    }

    AbstractGroovyTemplatePlugin(Class<? extends AbstractGroovyTemplateCompileTask> taskClass, String taskName, String fileExtension) {
        this.taskClass = taskClass
        this.taskName = taskName
        this.fileExtension = fileExtension
    }

    @Override
    void apply(Project project) {
        registerGrailsExtension(project)

        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)
        SourceSetOutput output = mainSourceSet?.output
        FileCollection classesDirs = resolveClassesDirs(output, project)

        File destDir = project.layout.buildDirectory.dir("classes/${this.fileExtension}/main").get().asFile

        Configuration compileClasspath = project.configurations.findByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
        FileCollection allClasspath = compileClasspath + classesDirs

        TaskContainer tasks = project.tasks
        String upperCaseName = GrailsNameUtils.getClassName(this.fileExtension)
        String viewsTaskName = this.taskName ?: "compile${upperCaseName}Views".toString()

        tasks.register(viewsTaskName, (Class<AbstractGroovyTemplateCompileTask>) taskClass) { AbstractGroovyTemplateCompileTask templateCompileTask ->
            templateCompileTask.group = 'Grace'
            templateCompileTask.destinationDirectory.set(destDir)
            templateCompileTask.classpath = allClasspath
            templateCompileTask.packageName = project.name
            templateCompileTask.dependsOn(tasks.named(JavaPlugin.CLASSES_TASK_NAME))
        }

        tasks.withType(AbstractGroovyTemplateCompileTask).configureEach { AbstractGroovyTemplateCompileTask templateCompileTask ->
            GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            if (grailsAppPath) {
                templateCompileTask.configDir = project.file("${grailsAppPath}/conf")
                templateCompileTask.source = project.file("${grailsAppPath}/views")
            }
            if (grailsExt.pathingJar && Os.isFamily(Os.FAMILY_WINDOWS)) {
                Jar pathingJar = (Jar) tasks.findByName('pathingJar')
                allClasspath = project.files(
                        project.layout.buildDirectory.dir("classes/groovy/main"),
                        project.layout.buildDirectory.dir("resources/main"),
                        pathingJar.archiveFile.get().getAsFile())
                templateCompileTask.dependsOn(pathingJar)
            }
        }

        tasks.withType(War).configureEach { War war ->
            war.dependsOn(tasks.withType(AbstractGroovyTemplateCompileTask))
            if (war.classpath) {
                war.classpath = war.classpath + project.files(destDir)
            } else {
                war.classpath = project.files(destDir)
            }
        }
        tasks.withType(Jar).configureEach { Jar jar ->
            if (!(jar instanceof War)) {
                if (jar.name == 'bootJar') {
                    jar.dependsOn(tasks.withType(AbstractGroovyTemplateCompileTask))
                    jar.from(destDir) { CopySpec spec ->
                        spec.into('BOOT-INF/classes')
                    }
                } else if (jar.name == 'jar') {
                    jar.dependsOn(tasks.withType(AbstractGroovyTemplateCompileTask))
                    jar.from destDir
                }
            }
        }
    }

    protected void registerGrailsExtension(Project project) {
        if (project.extensions.findByName(GrailsGradlePlugin.GRAILS_EXTENSION_NAME) == null) {
            project.extensions.add(GrailsGradlePlugin.GRAILS_EXTENSION_NAME, new GrailsExtension(project))
        }
    }

    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(project.layout.buildDirectory.dir('classes/groovy/main'))
    }

}
