/*
 * Copyright 2012-2026 the original author or authors.
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
package org.grails.gradle

import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.work.DisableCachingByDefault

/**
 * Creates start scripts for launching Grace Shell applications.
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
@DisableCachingByDefault(
        because = "Not worth caching"
)
abstract class GraceCreateStartScripts extends CreateStartScripts {

    @Internal
    TextResource unixStartScriptTemplate

    @Internal
    TextResource windowsStartScriptTemplate

    @Internal
    Collection<String> projectArtifacts = []

    GraceCreateStartScripts() {
        setDescription('Creates OS specific scripts to run grace-shell as a JVM application.')
        mainClass.set('org.grails.cli.GrailsCli')
        applicationName = 'grace'
    }

    @TaskAction
    void generate() {
        def generator = new org.gradle.api.internal.plugins.StartScriptGenerator()
        generator.unixStartScriptGenerator.template = unixStartScriptTemplate
        generator.windowsStartScriptGenerator.template = windowsStartScriptTemplate
        generator.applicationName = getApplicationName()
        generator.mainClassName = getMainClass().get()
        generator.defaultJvmOpts = getDefaultJvmOpts()
        generator.optsEnvironmentVar = getOptsEnvironmentVar()
        generator.exitEnvironmentVar = getExitEnvironmentVar()
        generator.classpath = projectArtifacts + getClasspath().resolvedConfiguration.resolvedArtifacts.collect { artifact ->
            def dependency = artifact.moduleVersion.id
            String installedFile = "lib/$dependency.group/$dependency.name/jars/$artifact.file.name"
            if (dependency.group == 'org.graceframework') {
                installedFile = "dist/$artifact.file.name"
            }
            installedFile
        }
        generator.scriptRelPath = "bin/${getUnixScript().name}"
        generator.generateUnixScript(getUnixScript())
        generator.generateWindowsScript(getWindowsScript())
    }

}