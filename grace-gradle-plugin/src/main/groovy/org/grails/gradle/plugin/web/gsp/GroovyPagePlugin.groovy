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
package org.grails.gradle.plugin.web.gsp

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War

import org.grails.gradle.plugin.core.GrailsExtension
import org.grails.gradle.plugin.core.GrailsGradlePlugin
import org.grails.gradle.plugin.util.SourceSets

/**
 * A plugin that adds support for compiling Groovy Server Pages (GSP)
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GroovyPagePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        registerGrailsExtension(project)

        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)

        SourceSetOutput output = mainSourceSet?.output
        FileCollection classesDirs = resolveClassesDirs(output, project)
        File destDir = project.layout.buildDirectory.dir('classes/gsp/main').get().asFile

        Configuration compileClasspath = project.configurations.findByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
        FileCollection allClasspath = compileClasspath + classesDirs

        TaskContainer tasks = project.tasks

        TaskProvider<GroovyPageForkCompileTask> compileWebappGroovyPages = tasks.register(
                'compileWebappGroovyPages', GroovyPageForkCompileTask) { GroovyPageForkCompileTask task ->
            task.group = 'grace'
            task.description = "Compiles the Groovy server pages (GSP) in 'src/main/webapp'."
            task.destinationDirectory.set(destDir)
            task.source = project.file("src/main/webapp")
            task.packageName = project.name
            task.tmpDirPath = getTmpDirPath(project)
            task.serverpath = '/'
            task.classpath = allClasspath
            task.dependsOn(tasks.named(JavaPlugin.CLASSES_TASK_NAME))
        }

        TaskProvider<GroovyPageForkCompileTask> compileGroovyPages = tasks.register(
                'compileGroovyPages', GroovyPageForkCompileTask) { GroovyPageForkCompileTask task ->
            task.group = 'grace'
            task.description = 'Compiles the Groovy server pages (GSP).'
            task.destinationDirectory.set(destDir)
            task.packageName = project.name
            task.tmpDirPath = getTmpDirPath(project)
            task.serverpath = '/WEB-INF/grails-app/views/'
            task.classpath = allClasspath
            task.dependsOn(tasks.named(JavaPlugin.CLASSES_TASK_NAME))
            task.dependsOn(compileWebappGroovyPages)
        }

        tasks.withType(GroovyPageForkCompileTask).configureEach { GroovyPageForkCompileTask task ->
            GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            if (grailsAppPath) {
                task.configDir = project.file("${grailsAppPath}/conf")
                if (task.name == 'compileGroovyPages') {
                    task.source = project.file("${grailsAppPath}/views")
                }
            }

            if (grailsExt.getPathingJar() && Os.isFamily(Os.FAMILY_WINDOWS)) {
                Jar pathingJar = (Jar) tasks.findByName('pathingJar')
                allClasspath = project.files(project.layout.buildDirectory.dir('classes/groovy/main'),
                        project.layout.buildDirectory.dir('resources/main'), pathingJar.archiveFile.get().getAsFile())
                task.dependsOn(pathingJar)
                task.setClasspath(allClasspath)
            }
        }

        tasks.withType(War) { War war ->
            war.dependsOn compileGroovyPages
            if (war.classpath) {
                war.classpath = war.classpath + project.files(destDir)
            }
            else {
                war.classpath = project.files(destDir)
            }
        }
        tasks.withType(Jar) { Jar jar ->
            if (!(jar instanceof War)) {
                if (jar.name == 'bootJar') {
                    jar.dependsOn compileGroovyPages
                    jar.from(destDir) {
                        into('BOOT-INF/classes')
                    }
                }
                else if (jar.name == 'jar') {
                    jar.dependsOn compileGroovyPages
                    jar.from destDir
                }
            }
        }
    }

    protected GrailsExtension registerGrailsExtension(Project project) {
        if (project.extensions.findByName(GrailsGradlePlugin.GRAILS_EXTENSION_NAME) == null) {
            project.extensions.add(GrailsGradlePlugin.GRAILS_EXTENSION_NAME, new GrailsExtension(project))
        }
    }

    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(project.layout.buildDirectory.dir('classes/groovy/main'))
    }

    protected String getTmpDirPath(Project project) {
        project.layout.buildDirectory.dir('tmp/gsp').get().asFile.absolutePath
    }

}
