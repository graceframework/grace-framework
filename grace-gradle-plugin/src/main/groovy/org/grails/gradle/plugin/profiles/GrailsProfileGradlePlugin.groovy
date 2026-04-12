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
package org.grails.gradle.plugin.profiles

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar

import grails.io.IOUtils

import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.gradle.plugin.profiles.tasks.ProfileCompilerTask
import org.grails.gradle.plugin.profiles.tasks.ProfileResourcesProcessTask

/**
 * A plugin that is capable of compiling a Grails profile into a JAR file for distribution
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
class GrailsProfileGradlePlugin implements Plugin<Project> {

    public static final String GRAILS_CONFIGURATION_NAME = 'grails'
    public static final String PROFILE_CONFIGURATION_NAME = 'profile'
    public static final String PROCESS_PROFILE_RESOURCES_TASK_NAME = 'processProfileResources'
    public static final String COMPILE_PROFILE_TASK_NAME = 'compileProfile'
    public static final String PROFILE_GROUP = 'profile'

    @Override
    void apply(Project project) {
        project.pluginManager.apply(GroovyPlugin)
        project.configurations.create(GRAILS_CONFIGURATION_NAME)

        Configuration profileConfiguration = project.configurations.create(PROFILE_CONFIGURATION_NAME)
        profileConfiguration.setCanBeConsumed(false)
        profileConfiguration.setCanBeResolved(true)
        profileConfiguration.setVisible(false)

        SourceSetContainer sourceSets = project.extensions.getByType(JavaPluginExtension).sourceSets
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

        project.plugins.withType(GroovyPlugin).configureEach {
            sourceSets.configureEach { SourceSet sourceSet ->
                project.configurations.getByName(sourceSet.compileClasspathConfigurationName)
                        .extendsFrom(profileConfiguration)
                project.configurations.getByName(sourceSet.implementationConfigurationName)
                        .extendsFrom(profileConfiguration)
                project.configurations.getByName(sourceSet.runtimeClasspathConfigurationName)
                        .extendsFrom(profileConfiguration)
            }
        }

        Provider<Directory> resourcesDir = project.layout.buildDirectory.dir('resources/profile')
        Provider<Directory> classesDir = project.layout.buildDirectory.dir('classes/profile')

        RegularFile profileYml = project.layout.projectDirectory.file('profile.yml')
        Directory commandsDir = project.layout.projectDirectory.dir('commands')
        Directory templatesDir = project.layout.projectDirectory.dir('templates')
        Directory skeletonsDir = project.layout.projectDirectory.dir('skeleton')
        Directory featuresDir = project.layout.projectDirectory.dir('features')

        TaskContainer tasks = project.tasks
        TaskProvider<ProfileResourcesProcessTask> processProfileResources = tasks.register(PROCESS_PROFILE_RESOURCES_TASK_NAME,
                ProfileResourcesProcessTask) { ProfileResourcesProcessTask task ->
            task.setGroup(PROFILE_GROUP)
            task.setDescription('Processes the Grace Profile resources.')
            task.commandsDir.set(commandsDir)
            task.featuresDir.set(featuresDir)
            task.skeletonDir.set(skeletonsDir)
            task.templatesDir.set(templatesDir)
            task.destinationDir.set(resourcesDir)
        }

        tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {
            it.dependsOn(processProfileResources)
        }

        TaskProvider<ProfileCompilerTask> compileProfileTask = tasks.register(COMPILE_PROFILE_TASK_NAME,
                ProfileCompilerTask) { ProfileCompilerTask task ->
            ResolvableDependencies profileDependencies = profileConfiguration.getIncoming()
            task.profileDependencyRoot.set(profileDependencies.resolutionResult.rootComponent)
            task.destinationDirectory.set(classesDir)
            task.source = commandsDir
            task.profileConfig.set(profileYml)
            task.profileFile.set(classesDir.get().file('META-INF/grails-profile/profile.yml'))
            task.featuresDir.set(featuresDir)
            task.templatesDir.set(templatesDir)
            task.classpath = profileConfiguration + project.files(IOUtils.findJarFile(GroovyScriptCommand))
            task.setGroup(PROFILE_GROUP)
            task.setDescription('Compiles the Grace Profile source.')
        }

        Provider<Directory> groovyClassesDir = project.layout.buildDirectory.dir('classes/groovy/main')
        TaskProvider<Task> compileTask = tasks.named('compileGroovy')
        compileTask.configure { it.dependsOn(compileProfileTask) }

        Provider<Jar> jarTask = registerOrGetJarTask(mainSourceSet, tasks)
        jarTask.configure { Jar jar ->
            jar.dependsOn(processProfileResources, compileTask)
            jar.from(resourcesDir)
            jar.from(classesDir)
            jar.from(groovyClassesDir)
            jar.destinationDirectory.set(project.layout.buildDirectory.dir('libs'))
        }

        Provider<Jar> sourcesJarTask = registerOrGetSourcesJarTask(mainSourceSet, tasks)
        sourcesJarTask.configure { Jar jar ->
            jar.from(commandsDir) { CopySpec spec ->
                spec.into('commands')
            }
            jar.from(templatesDir) { CopySpec spec ->
                spec.into('templates')
            }
            jar.from(skeletonsDir) { CopySpec spec ->
                spec.into('skeleton')
            }
            if (profileYml.getAsFile().exists()) {
                jar.from(profileYml)
            }
            jar.archiveClassifier.set('sources')
            jar.destinationDirectory.set(project.layout.buildDirectory.dir('libs'))
        }

        project.artifacts.add(GRAILS_CONFIGURATION_NAME, jarTask,
                { ConfigurablePublishArtifact artifact -> artifact.builtBy(jarTask) })

        if (tasks.names.contains(BasePlugin.ASSEMBLE_TASK_NAME)) {
            tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure { it.dependsOn(jarTask, sourcesJarTask) }
        }
    }

    private TaskProvider<Jar> registerOrGetJarTask(SourceSet sourceSet, TaskContainer tasks) {
        String jarTaskName = sourceSet.jarTaskName
        if (!tasks.names.contains(jarTaskName)) {
            return tasks.register(jarTaskName, Jar) { Jar jar ->
                jar.setDescription('Assembles a jar archive containing the Grace Profile classes and resources.')
                jar.setGroup(PROFILE_GROUP)
                jar.from(sourceSet.output)
            }
        }
        return tasks.named(jarTaskName, Jar)
    }

    private TaskProvider<Jar> registerOrGetSourcesJarTask(SourceSet sourceSet, TaskContainer tasks) {
        String jarTaskName = sourceSet.sourcesJarTaskName
        if (!tasks.names.contains(jarTaskName)) {
            return tasks.register(jarTaskName, Jar) { Jar jar ->
                jar.setDescription('Assembles a jar archive containing the Grace Profile sources and resources.')
                jar.setGroup(PROFILE_GROUP)
                jar.from(sourceSet.allSource)
            }
        }
        return tasks.named(jarTaskName, Jar)
    }

}
