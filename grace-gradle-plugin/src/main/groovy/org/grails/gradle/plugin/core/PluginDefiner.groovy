/*
 * Copyright 2014-2022 the original author or authors.
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
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency

import grails.util.BuildSettings
import grails.util.Environment

/**
 * Makes it easier to define Grails plugins and also makes them aware of the development environment
 * so that they can be run inline without creating a JAR
 *
 * @author Graeme Rocher
 * @since 3.2
 */
@PackageScope
class PluginDefiner {

    final Project project
    final exploded

    PluginDefiner(Project project, boolean exploded = true) {
        this.project = project
        this.exploded = exploded
    }

    void methodMissing(String name, args) {
        Object[] argArray = (Object[]) args

        if (argArray) {
            if (argArray[0] instanceof Map) {
                Map notation = (Map) argArray[0]
                if (!notation.containsKey('group')) {
                    notation.put('group', 'org.grails.plugins')
                }
            }
            else if (argArray[0] instanceof CharSequence) {
                String str = argArray[0]

                if (str.startsWith(':')) {
                    argArray[0] = "org.grails.plugins$str".toString()
                }
            }
            else if (Environment.isDevelopmentRun() && (argArray[0] instanceof ProjectDependency)) {
                ProjectDependency pd = argArray[0]
                project.dependencies.add(name, project.files(new File(pd.dependencyProject.projectDir, BuildSettings.BUILD_RESOURCES_PATH)))
            }
            project.dependencies.add(name, *argArray)
        }
        else {
            throw new MissingMethodException(name, GrailsExtension, args)
        }
    }

    @CompileStatic
    Dependency project(String path) {
        if (Environment.isDevelopmentRun()) {
            project.dependencies.project(path: path, configuration: 'exploded')
        }
        else {
            project.dependencies.project(path: path)
        }
    }

}
