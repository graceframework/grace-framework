/*
 * Copyright 2014-2026 the original author or authors.
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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import javax.inject.Inject

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.work.InputChanges

/**
 * Abstract Gradle task for compiling templates, using GroovyPageCompilerForkTask
 * This Task is a Forked Java Task that is configurable with fork options provided
 * by {@link GspCompileOptions}
 *
 * @author David Estes
 * @author Michael Yan
 * @since 4.0
 */
@CompileStatic
abstract class GroovyPageForkCompileTask extends DefaultTask {

    private ExecOperations execOperations

    @Nested
    GspCompileOptions compileOptions = getObjectFactory().newInstance(GspCompileOptions.class)

    @Input
    @Optional
    String packageName

    @Internal
    File srcDir

    @Internal
    File destDir

    @Internal
    File configDir

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

    @Input
    @Optional
    String targetCompatibility = '17'

    @Classpath
    FileCollection classpath

    @Inject
    GroovyPageForkCompileTask(ExecOperations execOperations) {
        this.execOperations = execOperations
    }

    @TaskAction
    void execute(InputChanges inputs) {
        compile()
    }

    protected void compile() {
        ExecResult result = this.execOperations.javaexec(
                new Action<JavaExecSpec>() {

                    @Override
                    @CompileDynamic
                    void execute(JavaExecSpec javaExecSpec) {
                        javaExecSpec.getMainClass().set(getCompilerName())
                        javaExecSpec.setClasspath(getClasspath())

                        List<String> jvmArgs = compileOptions.forkOptions.jvmArgs
                        if (jvmArgs) {
                            javaExecSpec.jvmArgs(jvmArgs)
                        }
                        javaExecSpec.setMaxHeapSize(compileOptions.forkOptions.memoryMaximumSize)
                        javaExecSpec.setMinHeapSize(compileOptions.forkOptions.memoryInitialSize)

                        String configFiles = [
                                new File(configDir, 'application.yml').canonicalPath,
                                new File(configDir, 'application.groovy').canonicalPath
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
                                destDir.canonicalPath,
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
