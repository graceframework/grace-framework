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
package org.grails.gradle.plugin.util

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class SourceSets {

    /**
     * Finds the main SourceSet for the project
     * @param project The project
     * @return The main source set or null if it can't be found
     */
    static SourceSet findMainSourceSet(Project project) {
        findSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME)
    }

    /**
     * Finds the main SourceSet for the project
     * @param project The project
     * @return The main source set or null if it can't be found
     */
    static SourceSet findSourceSet(Project project, String name) {
        SourceSetContainer sourceSets = findSourceSets(project)
        sourceSets?.find { SourceSet sourceSet ->
            sourceSet.name == name
        } as SourceSet
    }

    static SourceSetContainer findSourceSets(Project project) {
        JavaPluginExtension plugin = project.getExtensions().getByType(JavaPluginExtension)
        SourceSetContainer sourceSets = plugin?.sourceSets
        sourceSets
    }

    static String resolveGrailsAppDir(Project project) {
        List<String> grailsAppDirs = ['grails-app', 'app']
        String grailsAppDir = grailsAppDirs.find { String dir -> project.file(dir).exists() }
        if (!grailsAppDir) {
            throw new GradleException("Grails requires an application directory : 'grails-app' or 'app'.")
        }
        grailsAppDir
    }

}
