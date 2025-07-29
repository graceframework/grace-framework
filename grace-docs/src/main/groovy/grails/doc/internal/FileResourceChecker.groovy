/*
 * Copyright 2011-2025 the original author or authors.
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
package grails.doc.internal

import groovy.transform.CompileStatic

/**
 * Simple class that checks whether a path relative to a base directory exists
 * or not. Each instance of the class can have its own base directory.
 */
@CompileStatic
class FileResourceChecker implements ResourceChecker {

    private final File baseDir

    FileResourceChecker(File baseDir) {
        this.baseDir = baseDir
    }

    /**
     * To check whether the file resource exists
     *
     * @param path the file path
     * @return The path whether exists
     */
    boolean exists(String path) {
        new File(baseDir, path).exists()
    }

}
