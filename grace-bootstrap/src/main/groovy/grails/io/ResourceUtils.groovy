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
package grails.io

import groovy.transform.CompileStatic
import groovy.transform.Memoized

import grails.util.BuildSettings

/**
 * Utility methods for interacting with resources
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ResourceUtils {

    /**
     * Obtains the package names for the project. Works at development time only, do not use at runtime.
     *
     * @return The project package names
     */
    static Iterable<String> getProjectPackageNames() {
        getProjectPackageNames(BuildSettings.BASE_DIR)
    }

    @Memoized
    static Iterable<String> getProjectPackageNames(File baseDir) {
        File rootDir = baseDir ? new File(baseDir, 'grails-app') : null
        Set<String> packageNames = []
        if (rootDir?.exists()) {
            File[] allFiles = rootDir.listFiles()
            rootDir.eachDir { File dir ->
                String dirName = dir.name
                if (!dir.hidden && !dirName.startsWith('.') && !['conf', 'i18n', 'assets', 'views', 'migrations'].contains(dirName)) {
                    File[] files = dir.listFiles()
                    populatePackages(dir, files, packageNames, '')
                }
            }
        }

        packageNames
    }

    protected static populatePackages(File rootDir, File[] files, Collection<String> packageNames, String prefix) {
        if (files != null) {
            for (dir in files) {
                if (dir.isDirectory()) {
                    String dirName = dir.name
                    if (!dir.hidden && !dirName.startsWith('.')) {
                        File[] dirFiles = dir.listFiles()
                        if (dirFiles != null) {
                            boolean hasGroovySources = dirFiles?.find { File f -> f.name.endsWith('.groovy') }
                            if (hasGroovySources) {
                                // if there are Groovy sources stop here, no need to add child packages
                                packageNames.add "${prefix}${dirName}".toString()
                            }
                            else {
                                // otherwise recurse into a child package
                                populatePackages(dir, dirFiles, packageNames, "${prefix}${dirName}.")
                            }
                        }
                    }
                }
                else {
                    if (dir.name.endsWith('.groovy') && prefix == '') {
                        packageNames.add('')
                    }
                }
            }
        }
    }

}
