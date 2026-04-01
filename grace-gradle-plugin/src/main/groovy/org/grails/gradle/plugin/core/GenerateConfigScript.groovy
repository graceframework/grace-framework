/*
 * Copyright 2022-2026 the original author or authors.
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

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
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

    @Inject
    abstract FileSystemOperations getFs()

    @Input
    abstract Property<String> getProjectName()

    @Input
    abstract Property<String> getProjectVersion()

    @Input
    abstract Property<String> getProjectType()

    @Input
    abstract Property<String> getProjectDir()

    @Input
    @Optional
    abstract Property<String> getGrailsAppDir()

    @OutputFile
    abstract RegularFileProperty getConfigFile()

    @Input
    abstract MapProperty<String, String> getMetaDataMap()

    GenerateConfigScript() {
        Project project = getProject()
        getProjectName().convention(project.provider(project::getName))
    }

    @TaskAction
    void generateConfigScript() throws IOException {
        File configFile = getConfigFile().getAsFile().get()
        String projectDir = getProjectDir().get()
        String grailsAppDir = getGrailsAppDir().getOrNull()
        if (System.getProperty('os.name').startsWith('Windows')) {
            projectDir = projectDir?.replace('\\', '\\\\')
            grailsAppDir = grailsAppDir?.replace('\\', '\\\\')
        }

        // Default node metadata used for Groovy compiler
        Map<String, String> properties = [
                'PROJECT_NAME': getProjectName().get(),
                'PROJECT_TYPE': getProjectType().get(),
                'PROJECT_VERSION': getProjectVersion().get(),
                'PROJECT_DIR': projectDir
        ] as HashMap<String, String>

        if (grailsAppDir) {
            properties.put('GRAILS_APP_DIR', grailsAppDir)
        }

        // Add the user defined node metadata
        properties.putAll(getMetaDataMap().get())

        File groovyCompiler = new File(getProjectDir().get(), 'config/groovy/compiler.groovy')
        if (groovyCompiler.exists()) {
            fs.copy {
                it.from groovyCompiler
                it.into configFile.parentFile
                it.rename { configFile.name }
                it.expand(properties)
            }
            return
        }

        StringWriter sourceData = new StringWriter()
        properties.each { name, value ->
            sourceData.write("        source.ast.putNodeMetaData('${name}', '${value}')\r")
        }
        configFile.parentFile.mkdirs()
        configFile.text = """// A Groovy script file that configures the compiler, allowing extensive control over how the code is compiled.
// Please see the Groovy compiler customization builder documentation for more information about the compiler configuration DSL.
// https://docs.groovy-lang.org/latest/html/documentation/#compilation-customizers
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
${sourceData.toString()}
    }
}
"""
    }

}
