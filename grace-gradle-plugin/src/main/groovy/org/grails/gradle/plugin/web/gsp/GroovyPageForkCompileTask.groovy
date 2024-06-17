/*
 * Copyright 2014-2024 the original author or authors.
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

import org.gradle.api.model.ObjectFactory

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.work.InputChanges

import org.grails.gradle.plugin.util.SourceSets

/**
 * Abstract Gradle task for compiling templates, using GroovyPageCompilerForkTask
 * This Task is a Forked Java Task that is configurable with fork options provided
 * by {@link GspCompileOptions}
 *
 * @author David Estes
 * @since 4.0
 */
@CompileStatic
class GroovyPageForkCompileTask extends AbstractCompile {

    @Input
    @Optional
    String packageName

    @Internal
    File srcDir

    @LocalState
    String tmpDirPath

    /**
     * @deprecated Use {@link #tmpDirPath} instead.
     */
    @Deprecated
    @Optional
    @InputDirectory
    File tmpDir

    @Input
    @Optional
    String serverpath

    @Nested
    GspCompileOptions compileOptions = getObjectFactory().newInstance(GspCompileOptions.class)

    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    FileTree getSource() {
        super.getSource()
    }

    @Override
    void setSource(Object source) {
        try {
            srcDir = project.file(source)
            if (srcDir.exists() && !srcDir.isDirectory()) {
                throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
            }
            super.setSource(source)
        }
        catch (ignore) {
            throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
        }
    }

    @TaskAction
    void execute(InputChanges inputs) {
        compile()
    }

    protected void compile() {
        if (packageName == null) {
            packageName = project.name ?: project.projectDir.canonicalFile.name
        }

        String grailsAppDir = SourceSets.resolveGrailsAppDir(project)
        ExecResult result = project.javaexec(
                new Action<JavaExecSpec>() {

                    @Override
                    @CompileDynamic
                    void execute(JavaExecSpec javaExecSpec) {
                        javaExecSpec.getMainClass().set(getCompilerName())
                        javaExecSpec.setClasspath(getClasspath())

                        def jvmArgs = compileOptions.forkOptions.jvmArgs
                        if (jvmArgs) {
                            javaExecSpec.jvmArgs(jvmArgs)
                        }
                        javaExecSpec.setMaxHeapSize(compileOptions.forkOptions.memoryMaximumSize)
                        javaExecSpec.setMinHeapSize(compileOptions.forkOptions.memoryInitialSize)

                        //This is the OLD Style and seems kinda silly to be hard coded this way. but restores functionality
                        //for now
                        def configFiles = [
                                project.file("$grailsAppDir/conf/application.yml").canonicalPath,
                                project.file("$grailsAppDir/conf/application.groovy").canonicalPath
                        ].join(',')

                        Path path = Paths.get(tmpDirPath)
                        File tmp
                        if (Files.exists(path)) {
                            tmp = path.toFile()
                        }
                        else {
                            tmp = Files.createDirectories(path).toFile()
                        }
                        def arguments = [
                                srcDir.canonicalPath,
                                destinationDirectory.getAsFile().getOrNull()?.canonicalPath,
                                tmp.canonicalPath,
                                targetCompatibility,
                                packageName,
                                serverpath,
                                configFiles,
                                compileOptions.encoding
                        ]

                        prepareArguments(arguments)
                        javaExecSpec.args(arguments)
                    }

                }
        )
        result.assertNormalExitValue()
    }

    void prepareArguments(List<String> arguments) {
        // no-op
    }

    @Input
    protected String getCompilerName() {
        'org.grails.web.pages.GroovyPageCompilerForkTask'
    }

    @Input
    String getFileExtension() {
        'gsp'
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException()
    }

}
