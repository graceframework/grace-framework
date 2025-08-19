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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War

import org.grails.gradle.plugin.core.GrailsExtension
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

    @CompileDynamic
    @Override
    void apply(Project project) {
        registerGrailsExtension(project)

        project.configurations.create('gspCompile')
        project.dependencies.add('gspCompile', 'jakarta.servlet:jakarta.servlet-api:6.0.0')

        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)

        SourceSetOutput output = mainSourceSet?.output
        FileCollection classesDirs = resolveClassesDirs(output, project)
        File destDir = output?.dir('gsp-classes') ?: new File(project.buildDir, 'gsp-classes/main')

        Configuration providedConfig = project.configurations.findByName('providedCompile')
        def allClasspath = project.configurations.compileClasspath + project.configurations.gspCompile + classesDirs
        if (providedConfig) {
            allClasspath += providedConfig
        }

        TaskContainer tasks = project.tasks

        TaskProvider<GroovyPageForkCompileTask> compileWebappGroovyPages = tasks.register(
                'compileWebappGroovyPages', GroovyPageForkCompileTask) { GroovyPageForkCompileTask task ->
            task.group = 'grace'
            task.description = "Compiles the Groovy server pages (GSP) in 'src/main/webapp'."
            task.destinationDirectory.set(destDir)
            task.source = project.file("src/main/webapp")
            task.tmpDirPath = getTmpDirPath(project)
            task.serverpath = '/'
            task.classpath = allClasspath
        }

        TaskProvider<GroovyPageForkCompileTask> compileGroovyPages = tasks.register(
                'compileGroovyPages', GroovyPageForkCompileTask) { GroovyPageForkCompileTask task ->
            task.group = 'grace'
            task.description = 'Compiles the Groovy server pages (GSP).'
            task.destinationDirectory.set(destDir)
            task.tmpDirPath = getTmpDirPath(project)
            task.classpath = allClasspath
            task.dependsOn(tasks.named('classes'))
            task.dependsOn(compileWebappGroovyPages)
        }

        tasks.withType(GroovyPageForkCompileTask).configureEach { GroovyPageForkCompileTask task ->
            GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            if (grailsAppPath) {
                task.source = project.file("${grailsAppPath}/views")
                task.serverpath = '/WEB-INF/grails-app/views/'
            }

            if (grailsExt.getPathingJar() && Os.isFamily(Os.FAMILY_WINDOWS)) {
                Jar pathingJar = (Jar) tasks.findByName('pathingJar')
                allClasspath = project.files("${project.buildDir}/classes/groovy/main",
                        "${project.buildDir}/resources/main", pathingJar.archiveFile.get().getAsFile())
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
        if (project.extensions.findByName('grails') == null) {
            project.extensions.add('grails', new GrailsExtension(project))
        }
    }

    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(new File(project.buildDir, 'classes/main'))
    }

    protected String getTmpDirPath(Project project) {
        def tmpdir = new File(project.buildDir as String, 'gsptmp')
        tmpdir.absolutePath
    }

}
