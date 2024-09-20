/*
 * Copyright 2015-2024 the original author or authors.
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
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar

import grails.io.IOUtils

import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.gradle.plugin.profiles.tasks.ProfileCompilerTask

import static org.gradle.api.plugins.BasePlugin.BUILD_GROUP

/**
 * A plugin that is capable of compiling a Grails profile into a JAR file for distribution
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
class GrailsProfileGradlePlugin implements Plugin<Project> {

    static final String CONFIGURATION_NAME = 'grails'

    public static final String RUNTIME_CONFIGURATION = 'profile'

    @Override
    void apply(Project project) {
        project.getPluginManager().apply(GroovyPlugin)
        project.configurations.create(CONFIGURATION_NAME)
        Configuration profileConfiguration = project.configurations.create(RUNTIME_CONFIGURATION)
        profileConfiguration.setCanBeConsumed(false)
        profileConfiguration.setCanBeResolved(true)
        profileConfiguration.setVisible(false)
        project.getPlugins().withType(GroovyPlugin, { javaPlugin ->
            SourceSetContainer sourceSets = project.getExtensions()
                    .getByType(JavaPluginExtension).getSourceSets()
            sourceSets.configureEach { SourceSet sourceSet ->
                project.getConfigurations()
                        .getByName(sourceSet.getCompileClasspathConfigurationName())
                        .extendsFrom(profileConfiguration)
                project.getConfigurations()
                        .getByName(sourceSet.getImplementationConfigurationName())
                        .extendsFrom(profileConfiguration)
                project.getConfigurations()
                        .getByName(sourceSet.getRuntimeClasspathConfigurationName())
                        .extendsFrom(profileConfiguration)
            }
        })

        def profileYml = project.file('profile.yml')

        def commandsDir = project.file('commands')
        def resourcesDir = new File(project.buildDir, 'resources/profile')
        def templatesDir = project.file('templates')
        def skeletonsDir = project.file('skeleton')
        def featuresDir = project.file('features')

        def spec1 = project.copySpec { CopySpec spec ->
            spec.from(commandsDir)
            spec.exclude('*.groovy')
            spec.into('commands')
        }
        def spec2 = project.copySpec { CopySpec spec ->
            spec.from(templatesDir)
            spec.into('templates')
        }
        def spec4 = project.copySpec { CopySpec spec ->
            spec.from(featuresDir)
            spec.into('features')
        }
        def spec3 = project.copySpec { CopySpec spec ->
            spec.from(skeletonsDir)
            spec.into('skeleton')
        }

        def processProfileResources = project.tasks.create('processProfileResources', Copy, (Action) { Copy c ->
            c.with(spec1, spec2, spec3, spec4)
            c.into(new File(resourcesDir, '/META-INF/grails-profile'))
        })

        def classesDir = new File(project.buildDir, 'classes/profile')
        def compileProfileTask = project.tasks.create('compileProfile', ProfileCompilerTask, (Action) { ProfileCompilerTask task ->
            task.destinationDirectory.set(classesDir)
            task.source = commandsDir
            task.config = profileYml
            task.profileFile = new File(classesDir, 'META-INF/grails-profile/profile.yml')
            if (templatesDir.exists()) {
                task.templatesDir = templatesDir
            }
            task.classpath = project.configurations.getByName(RUNTIME_CONFIGURATION) + project.files(IOUtils.findJarFile(GroovyScriptCommand))
        })

        def groovyClassesDir = new File(project.buildDir, 'classes/groovy/main')
        def compileTask = project.tasks.getByName('compileGroovy')
        if (compileTask) {
            compileTask.dependsOn(compileProfileTask)
        }

        Jar jarTask = (Jar) project.tasks.getByName('jar')

        if (jarTask) {
            jarTask.dependsOn(processProfileResources, compileTask)
            jarTask.from(resourcesDir)
            jarTask.from(classesDir)
            jarTask.from(groovyClassesDir)
            jarTask.destinationDirectory.set(new File(project.buildDir, 'libs'))
            jarTask.setDescription('Assembles a jar archive containing the profile classes.')
            jarTask.setGroup(BUILD_GROUP)
        }
        else {
            // Create jar task
            jarTask = project.tasks.create('jar', Jar, (Action) { Jar jar ->
                jar.dependsOn(processProfileResources, compileTask)
                jar.from(resourcesDir)
                jar.from(classesDir)
                jar.from(groovyClassesDir)
                jar.destinationDirectory.set(new File(project.buildDir, 'libs'))
                jar.setDescription('Assembles a jar archive containing the profile classes.')
                jar.setGroup(BUILD_GROUP)
            })
        }

        project.artifacts.add(CONFIGURATION_NAME, jarTask.getArchiveFile(),
                { ConfigurablePublishArtifact artifact -> artifact.builtBy(jarTask) })

        project.tasks.create('sourcesJar', Jar, (Action) { Jar jar ->
            jar.from(commandsDir)
            if (profileYml.exists()) {
                jar.from(profileYml)
            }
            jar.from(templatesDir) { CopySpec spec ->
                spec.into('templates')
            }
            jar.from(skeletonsDir) { CopySpec spec ->
                spec.into('skeleton')
            }
            jar.archiveClassifier.set('sources')
            jar.destinationDirectory.set(new File(project.buildDir, 'libs'))
            jar.setDescription('Assembles a jar archive containing the profile sources.')
            jar.setGroup(BUILD_GROUP)
        })

        project.tasks.findByName('assemble').dependsOn jarTask
    }

}
