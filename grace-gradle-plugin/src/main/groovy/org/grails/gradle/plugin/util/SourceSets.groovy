/*
 * Copyright 2014-2025 the original author or authors.
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

import org.grails.gradle.plugin.core.GrailsExtension

import static org.grails.gradle.plugin.util.BuildSettings.APP_DIR
import static org.grails.gradle.plugin.util.BuildSettings.APP_PATH
import static org.grails.gradle.plugin.util.BuildSettings.DEFAULT_GRACE_APP_PATH
import static org.grails.gradle.plugin.util.BuildSettings.DEFAULT_GRAILS_APP_PATH

/**
 * @author Graeme Rocher
 * @author Michael Yan
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

    /**
     * Resolve the directory of Grails app, for examples, 'grails-app', 'app'.
     *
     * @param project the Gradle project
     * @return the directory of Grails application
     * @deprecated since 2024.0.0, in favor of {@link #resolveGrailsAppPath(Project)}
     */
    @Deprecated(since = '2024.0.0', forRemoval = true)
    static String resolveGrailsAppDir(Project project) {
        List<String> grailsAppDirs = [DEFAULT_GRAILS_APP_PATH, DEFAULT_GRACE_APP_PATH]
        String grailsAppDir = grailsAppDirs.find { String dir -> project.file(dir).exists() }
        if (!grailsAppDir) {
            throw new GradleException("Grace requires an application directory : 'grails-app' or 'app'.")
        }
        grailsAppDir
    }

    /**
     * Resolve the directory of Grails app, for examples, 'grails-app', 'app', but you can always configure it.
     *
     * @param project the Gradle project
     * @return the directory of Grails application
     * @since 2024.0.0
     */
    static String resolveGrailsAppPath(Project project) {
        File baseDir = project.projectDir
        File appDir = null

        if (System.getProperty(APP_DIR)) {
            appDir = new File(System.getProperty(APP_DIR))
        }
        else if (System.getenv(APP_DIR)) {
            appDir = new File(System.getenv(APP_DIR))
        }
        else if (System.getProperty('GRAILS_APP_DIR')) {
            appDir = new File(System.getProperty('GRAILS_APP_DIR'))
        }
        else if (System.getenv('GRAILS_APP_DIR')) {
            appDir = new File(System.getenv('GRAILS_APP_DIR'))
        }
        else if (System.getProperty(APP_PATH)) {
            appDir = new File(baseDir, System.getProperty(APP_PATH))
        }
        else if (System.getenv(APP_PATH)) {
            appDir = new File(baseDir, System.getenv(APP_PATH))
        }
        else if (System.getProperty('GRAILS_APP_PATH')) {
            appDir = new File(baseDir, System.getProperty('GRAILS_APP_PATH'))
        }
        else if (System.getenv('GRAILS_APP_PATH')) {
            appDir = new File(baseDir, System.getenv('GRAILS_APP_PATH'))
        }
        if (appDir?.exists()) {
            return appDir.absolutePath.substring(baseDir.absolutePath.length() + 1)
        }
        String appPath = project.getExtensions().getByType(GrailsExtension).appPath?.trim()
        if (!appPath) {
            return ''
        }
        else {
            appDir = new File(baseDir, appPath)
        }

        if (appDir.exists()) {
            return appPath
        }
        else if (appPath != DEFAULT_GRACE_APP_PATH && System.getProperty('grails.gradle.app-path.checked') != 'true') {
            System.setProperty('grails.gradle.app-path.checked', 'true')
            throw new GradleException("Grace requires an application directory : [$appDir] not exist, please check it again!")
        }
        else {
            return ''
        }
    }

}
