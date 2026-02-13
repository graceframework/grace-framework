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

import org.grails.config.CodeGenConfig

import javax.inject.Inject

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.work.InputChanges

/**
 * Abstract Gradle task for compiling templates, using GenericGroovyTemplateCompiler
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
abstract class AbstractGroovyTemplateCompileTask extends AbstractCompile {

    protected ExecOperations execOperations

    @Input
    @Optional
    String packageName

    @InputDirectory
    File srcDir

    @InputDirectory
    File configDir

    @Nested
    ViewCompileOptions compileOptions = getObjectFactory().newInstance(ViewCompileOptions)

    @Inject
    AbstractGroovyTemplateCompileTask(ExecOperations execOperations) {
        this.execOperations = execOperations
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException()
    }

    @Override
    void setSource(Object source) {
        try {
            this.srcDir = project.file(source)
            if (this.srcDir.exists() && !this.srcDir.isDirectory()) {
                throw new IllegalArgumentException("The source for Views compilation must be a single directory, but was $source")
            }
            super.setSource(source)
        } catch (ignore) {
            throw new IllegalArgumentException("The source for Views compilation must be a single directory, but was $source")
        }
    }

    @TaskAction
    void execute(InputChanges inputs) {
        compile()
    }

    protected void compile() {
        if (this.packageName == null) {
            this.packageName = project.name
        }

        ExecResult result = this.execOperations.javaexec(
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

                        String packageImports = getDefaultPackageName() ?: packageName
                        def arguments = [
                                srcDir.canonicalPath,
                                destinationDirectory.getAsFile().get()?.canonicalPath,
                                targetCompatibility,
                                packageImports,
                                packageName,
                                new File(configDir, 'application.yml').canonicalPath,
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
        'grails.views.GenericGroovyTemplateCompiler'
    }

    @Input
    protected String getDefaultPackageName() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File(this.configDir, 'application.yml')
        if (applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        return config.getProperty('grails.codegen.defaultPackage', String.class)
    }

    @Input
    abstract String getFileExtension()

    @Input
    abstract String getScriptBaseName()

}
