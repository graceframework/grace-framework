/*
 * Copyright 2022-2025 the original author or authors.
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

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generate the Groovy configuration script for {@link org.gradle.api.tasks.compile.GroovyCompile}
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
abstract class GenerateConfigScript extends DefaultTask {

    @Input
    abstract Property<String> getProjectName()

    @Input
    abstract Property<String> getProjectVersion()

    @Input
    abstract Property<String> getProjectType()

    @Input
    abstract Property<String> getProjectDir()

    @Input
    abstract Property<String> getGrailsAppDir()

    @OutputFile
    abstract RegularFileProperty getConfigFile()

    GenerateConfigScript() {
        Project project = getProject()
        getProjectName().convention(project.provider(project::getName))
    }

    @TaskAction
    void generateConfigScript() throws IOException {
        String projectDir = getProjectDir().get()
        String grailsAppDir = getGrailsAppDir().get()
        if (System.getProperty('os.name').startsWith('Windows')) {
            projectDir = projectDir.replace('\\', '\\\\')
            grailsAppDir = grailsAppDir.replace('\\', '\\\\')
        }
        File configFile = getConfigFile().getAsFile().get()
        configFile.parentFile.mkdirs()
        configFile.text = """// A Groovy script file that configures the compiler, allowing extensive control over how the code is compiled.
// Please see the Groovy compiler customization builder documentation for more information about the compiler configuration DSL.
// https://docs.groovy-lang.org/latest/html/documentation/#compilation-customizers
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        source.ast.putNodeMetaData('PROJECT_NAME', '${getProjectName().get()}')
        source.ast.putNodeMetaData('PROJECT_TYPE', '${getProjectType().get()}')
        source.ast.putNodeMetaData('PROJECT_VERSION', '${getProjectVersion().get()}')
        source.ast.putNodeMetaData('PROJECT_DIR', '${projectDir}')
        source.ast.putNodeMetaData('GRAILS_APP_DIR', '${grailsAppDir}')
    }
}
"""
    }

}
