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
package org.grails.gradle.plugin.web.gsp

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task for compiling GSP views in the app/views directory,
 * extended {@link GroovyPageForkCompileTask}.
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
@CompileStatic
@DisableCachingByDefault(because = "Not worth caching")
abstract class GroovyPageViewsForkCompileTask extends GroovyPageForkCompileTask {

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getViewsPropertiesFile()

    @Inject
    GroovyPageViewsForkCompileTask(ExecOperations execOperations) {
        super(execOperations)
    }

}
