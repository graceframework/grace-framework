/*
 * Copyright 2014-2023 the original author or authors.
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
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

/**
 * A extension to the Gradle plugin to configure Grails settings
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsExtension {

    Project project

    GrailsExtension(Project project) {
        this.project = project
    }

    /**
     * Whether to invoke native2ascii on resource bundles
     */
    boolean native2ascii = !Os.isFamily(Os.FAMILY_WINDOWS)

    /**
     * Whether to use Ant to do the conversion
     */
    boolean native2asciiAnt = false

    /**
     * Whether assets should be packaged in META-INF/assets for plugins
     */
    boolean packageAssets = true

    /**
     * Whether to include subproject dependencies as directories directly on the classpath, instead of as JAR files
     */
    boolean exploded = true

    /**
     * Whether to create a jar file to reference the classpath to prevent classpath too long issues in Windows
     */
    boolean pathingJar = false

    /**
     * Allows defining plugins in the available scopes
     */
    void plugins(Closure pluginDefinitions) {
        def definer = new PluginDefiner(project, exploded)
        project.configure(definer, pluginDefinitions)
    }

}
