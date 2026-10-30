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
package org.grails.gradle.plugin.profiles.tasks

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.SyncSpec
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * {@link Task} for processing the resources of the Profile
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
@DisableCachingByDefault(because = "Not worth caching")
abstract class ProfileResourcesProcessTask extends DefaultTask {

    private final FileSystemOperations fileSystemOperations

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getCommandsDir()

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getFeaturesDir()

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getSkeletonDir()

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getTemplatesDir()

    @OutputDirectory
    abstract DirectoryProperty getDestinationDir()

    @Inject
    ProfileResourcesProcessTask(FileSystemOperations fileSystemOperations) {
        this.fileSystemOperations = fileSystemOperations
    }

    @TaskAction
    void processResources() throws IOException {
        CopySpec copyCommands = this.fileSystemOperations.copySpec { spec ->
            spec.from(commandsDir)
            spec.exclude('*.groovy')
            spec.into('commands')
        }

        CopySpec copyFeatures = this.fileSystemOperations.copySpec { spec ->
            spec.from(featuresDir)
            spec.into('features')
        }

        CopySpec copySkeleton = this.fileSystemOperations.copySpec { spec ->
            spec.from(skeletonDir)
            spec.into('skeleton')
        }

        CopySpec copyTemplates = this.fileSystemOperations.copySpec { spec ->
            spec.from(templatesDir)
            spec.into('templates')
        }

        this.fileSystemOperations.sync { SyncSpec copy ->
            copy.with(copyCommands, copyFeatures, copySkeleton, copyTemplates)
            copy.into(destinationDir.dir('META-INF/grails-profile'))
        }
    }

}
