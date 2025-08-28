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
package org.grails.gradle.plugin.core

import javax.inject.Inject

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

import grails.util.Environment

import org.grails.gradle.plugin.util.SourceSets

/**
 * A Gradle plugin for Grails plugins
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GrailsPluginGradlePlugin extends GrailsGradlePlugin {

    @Inject
    GrailsPluginGradlePlugin(ToolingModelBuilderRegistry registry) {
        super(registry)
    }

    @Override
    void apply(Project project) {
        super.apply(project)

        checkForConfigurationClash(project)

        configureAstSources(project)

        configurePluginResources(project)

        configurePluginJarTask(project)

        configureSourcesJarTask(project)

        configureExplodedDirConfiguration(project)
    }

    @Override
    protected void applySpringBootPlugin(Project project) {
        super.applySpringBootPlugin(project)
        project.tasks.withType(BootJar).configureEach { BootJar bootJar ->
            bootJar.enabled = false
        }
        project.tasks.withType(BootRun).configureEach { BootRun bootRun ->
            bootRun.enabled = false
        }
    }

    protected void checkForConfigurationClash(Project project) {
        project.afterEvaluate {
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            if (grailsAppPath) {
                File yamlConfig = new File(project.projectDir, "${grailsAppPath}/conf/plugin.yml")
                File groovyConfig = new File(project.projectDir, "${grailsAppPath}/conf/plugin.groovy")
                if (yamlConfig.exists() && groovyConfig.exists()) {
                    throw new RuntimeException('A plugin may define a plugin.yml or a plugin.groovy, but not both')
                }
            }
        }
    }

    @Override
    protected void createBuildPropertiesTask(Project project) {
        // no-op
    }

    @CompileDynamic
    protected void configureAstSources(Project project) {
        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)
        SourceSetContainer sourceSets = SourceSets.findSourceSets(project)
        project.sourceSets {
            ast {
                groovy {
                    compileClasspath += project.configurations.compileClasspath
                }
            }
            main {
                compileClasspath += sourceSets.ast.output
            }
            test {
                compileClasspath += sourceSets.ast.output
            }
        }

        def copyAstClasses = project.task(type: Copy, 'copyAstClasses') {
            from sourceSets.ast.output
            into "${project.buildDir}/classes/groovy/main"
        }

        def taskContainer = project.tasks
        taskContainer.getByName('classes').dependsOn(copyAstClasses)

        taskContainer.withType(JavaExec) {
            classpath += sourceSets.ast.output
        }

        Task javadocTask = taskContainer.findByName('javadoc')
        Task groovydocTask = taskContainer.findByName('groovydoc')
        if (javadocTask) {
            javadocTask.configure {
                source += sourceSets.ast.allJava
            }
        }

        if (groovydocTask) {
            if (taskContainer.findByName('javadocJar') == null) {
                taskContainer.create('javadocJar', Jar).configure {
                    archiveClassifier = 'javadoc'
                    from groovydocTask.outputs
                }.dependsOn(javadocTask)
            }

            groovydocTask.configure {
                source += sourceSets.ast.allJava
            }
        }
    }

    @CompileDynamic
    protected void configurePluginResources(Project project) {
        project.afterEvaluate {
            ProcessResources processResources = (ProcessResources) project.tasks.getByName('processResources')
            def processResourcesDependencies = []
            processResourcesDependencies << project.tasks.register('copyCommands', Copy) {
                it.from "${project.projectDir}/src/main/scripts"
                it.into "${processResources.destinationDir}/META-INF/commands"
            }
            processResourcesDependencies << project.tasks.register('copyTemplates', Copy) {
                it.from "${project.projectDir}/src/main/templates"
                it.into "${processResources.destinationDir}/META-INF/templates"
            }

            processResources.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
            processResources.dependsOn(*processResourcesDependencies)
            processResources.exclude('spring/resources.groovy', '**/*.gsp')
        }
    }

    protected void configurePluginJarTask(Project project) {
        project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar).configure { Jar jarTask ->
            jarTask.enabled = true
            jarTask.archiveClassifier.set('plugin')
            jarTask.exclude('application.yml', 'application.groovy', 'logback.groovy', 'logback.xml', 'logback-spring.xml')
        }
    }

    @CompileStatic
    protected void configureSourcesJarTask(Project project) {
        def taskContainer = project.tasks
        if (taskContainer.findByName('sourcesJar') == null) {
            taskContainer.register('sourcesJar', Jar).configure { Jar jarTask ->
                jarTask.archiveClassifier.set('sources')
                jarTask.from SourceSets.findMainSourceSet(project).allSource
            }
        }
    }

    /**
     * Configures an exploded configuration that can be used to build the classpath of the application
     * from subprojects that are plugins without constructing a JAR file
     *
     * @param project The project instance
     */
    protected void configureExplodedDirConfiguration(Project project) {
        ConfigurationContainer allConfigurations = project.configurations

        def runtimeConfiguration = allConfigurations.findByName('runtimeClasspath')
        def explodedConfig = allConfigurations.create('exploded')
        explodedConfig.extendsFrom(runtimeConfiguration)
        if (Environment.isDevelopmentRun() && isExploded(project)) {
            runtimeConfiguration.artifacts.clear()
            // add the subproject classes as outputs
            TaskContainer allTasks = project.tasks

            GroovyCompile groovyCompile = (GroovyCompile) allTasks.findByName('compileGroovy')
            ProcessResources processResources = (ProcessResources) allTasks.findByName('processResources')

            runtimeConfiguration.artifacts.add(new ExplodedDir(groovyCompile.destinationDirectory.get().asFile, groovyCompile, processResources))
            explodedConfig.artifacts.add(new ExplodedDir(processResources.destinationDir, groovyCompile, processResources))
        }
    }

    @Override
    protected GrailsProjectType getGrailsProjectType() {
        GrailsProjectType.PLUGIN
    }

    @Override
    protected String getGrailsProjectName(Project project) {
        SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        SourceDirectorySet groovySourceSet = sourceSet.getExtensions().getByType(GroovySourceDirectorySet)
        File grailsPluginFile = groovySourceSet.getFiles().find { File f -> f.name.endsWith('GrailsPlugin.groovy') }
        if (grailsPluginFile) {
            return grailsPluginFile.name.substring(0, grailsPluginFile.name.indexOf('GrailsPlugin.groovy'))
        }
        return project.name
    }

    @Override
    protected String getDefaultProfile() {
        'web-plugin'
    }

    @CompileDynamic
    private boolean isExploded(Project project) {
        Boolean.valueOf(project.properties.getOrDefault('exploded', 'false').toString())
    }

    static class ExplodedDir implements PublishArtifact {

        final String extension = ''
        final String type = 'dir'
        final Date date = new Date()

        final File file
        final TaskDependency buildDependencies

        ExplodedDir(File file, Object... tasks) {
            this.file = file
            this.buildDependencies = new DefaultTaskDependency().add(tasks)
        }

        @Override
        String getName() {
            file.name
        }

        @Override
        String getClassifier() {
            ''
        }

    }

}
