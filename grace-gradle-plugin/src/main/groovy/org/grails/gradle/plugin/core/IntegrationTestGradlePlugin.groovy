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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.language.base.plugins.LifecycleBasePlugin

import org.grails.gradle.plugin.util.SourceSets

/**
 * Gradle plugin for adding separate src/integration-test folder to hold integration tests
 *
 * Adds integrationTestImplementation and integrationTestRuntimeOnly configurations
 * that extend from testCompileClasspath and testRuntimeClasspath.
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class IntegrationTestGradlePlugin implements Plugin<Project> {

    boolean ideaIntegration = true
    String sourceFolderName = 'src/integration-test'

    @Override
    void apply(Project project) {
        File[] sourceDirs = project.file(sourceFolderName).listFiles({ File file ->
            file.isDirectory() && !file.name.contains('.')
        } as FileFilter)

        if (sourceDirs) {
            List<File> acceptedSourceDirs = []
            SourceSetContainer sourceSets = SourceSets.findSourceSets(project)
            SourceSet integrationTestSources = sourceSets.create('integrationTest')

            for (File srcDir in sourceDirs) {
                registerSourceDir(integrationTestSources, srcDir)
                acceptedSourceDirs.add srcDir
            }

            DependencyHandler dependencies = project.dependencies
            dependencies.add('integrationTestImplementation', SourceSets.findMainSourceSet(project).output)
            dependencies.add('integrationTestImplementation', SourceSets.findSourceSet(project, SourceSet.TEST_SOURCE_SET_NAME).output)
            ConfigurationContainer configurations = project.configurations
            configurations.getByName('integrationTestImplementation').extendsFrom(configurations.getByName('testCompileClasspath'))
            configurations.getByName('integrationTestRuntimeOnly').extendsFrom(configurations.getByName('testRuntimeClasspath'))

            TaskContainer tasks = project.tasks
            TaskProvider<Test> testTask = tasks.named('test', Test)

            TaskProvider<TestReport> mergeTestReports = tasks.register('mergeTestReports', TestReport) { TestReport testReportTask ->
                testReportTask.dependsOn(testTask)

                testReportTask.getDestinationDirectory().set(project.layout.buildDirectory.dir('reports/tests'))
                testReportTask.getTestResults().from(project.layout.buildDirectory.dir('test-results/binary/test'),
                        project.layout.buildDirectory.dir('test-results/binary/integrationTest'),
                        // different versions of Gradle store these results in different places. ugh.
                        project.layout.buildDirectory.dir('test-results/test/binary'),
                        project.layout.buildDirectory.dir('test-results/integrationTest/binary'))
            }

            TaskProvider<Test> integrationTestTask = tasks.register('integrationTest', Test, { Test integrationTest ->
                integrationTest.group = LifecycleBasePlugin.VERIFICATION_GROUP
                integrationTest.description = 'Runs the integration tests.'
                integrationTest.setTestClassesDirs(integrationTestSources.output.classesDirs)
                integrationTest.classpath = integrationTestSources.runtimeClasspath
                integrationTest.maxParallelForks = 1
                integrationTest.reports.html.required.set(false)
            })
            integrationTestTask.configure { Test integrationTest ->
                integrationTest.shouldRunAfter(testTask)
                integrationTest.finalizedBy(mergeTestReports)
                integrationTest.doFirst {
                    String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
                    if (grailsAppPath) {
                        File resources = project.file("${grailsAppPath}/conf")
                        integrationTestSources.resources.srcDir(resources)
                    }
                }
            }
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME)
                    .configure((check) -> check.dependsOn(integrationTestTask))

            if (ideaIntegration) {
                integrateIdea(project, acceptedSourceDirs)
            }
        }
    }

    @CompileDynamic
    private void registerSourceDir(SourceSet integrationTest, File srcDir) {
        integrationTest."${srcDir.name}".srcDir srcDir
    }

    @CompileDynamic
    private integrateIdea(Project project, List<File> acceptedSourceDirs) {
        project.afterEvaluate {
            if (project.extensions.findByName('idea')) {
                // IDE integration for IDEA. Eclipse plugin already handles all source folders.
                project.idea {
                    module {
                        acceptedSourceDirs.each {
                            testSources.from(project.files(it))
                        }
                    }
                }
            }
        }
    }

}
