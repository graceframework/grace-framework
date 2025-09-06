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
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * {@link Task} for generating a {@code grails.build.info} file from a
 * {@code Project}.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
abstract class GenerateBuildInfo extends DefaultTask {

    private static final String BUILD_INFO_FILE = 'META-INF/grails.build.info'

    @Input
    abstract MapProperty<String, Object> getProperties()

    @OutputDirectory
    abstract DirectoryProperty getDestinationDir()

    @TaskAction
    void execute() {
        File buildInfoFile = new File(getDestinationDir().get().asFile, BUILD_INFO_FILE)
        buildInfoFile.parentFile.mkdir()

        Properties buildProperties = new Properties()
        buildProperties.putAll(getProperties().getOrNull())

        buildInfoFile.withOutputStream { OutputStream out ->
            buildProperties.store(out, null)
        }
    }

}
