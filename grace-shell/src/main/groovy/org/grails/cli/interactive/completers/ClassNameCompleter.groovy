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
package org.grails.cli.interactive.completers

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet

import groovy.transform.CompileStatic

import org.grails.io.support.PathMatchingResourcePatternResolver
import org.grails.io.support.Resource

/**
 * A completer that completes class names
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ClassNameCompleter extends StringsCompleter {

    private static final Map<String, SortedSet<String>> RESOURCE_SCAN_CACHE = [:]
    private static final Collection<ClassNameCompleter> ALL_COMPLETERS = new ConcurrentLinkedQueue<>()
    PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver()

    private File[] baseDirs

    ClassNameCompleter(File baseDir) {
        initialize(baseDir)
    }

    ClassNameCompleter(File... baseDirs) {
        initialize(baseDirs)
    }

    static void refreshAll() {
        Thread.start {
            RESOURCE_SCAN_CACHE.clear()
            Collection<ClassNameCompleter> competers = new ArrayList<>(ALL_COMPLETERS)
            for (ClassNameCompleter completer : competers) {
                completer.refresh()
            }
        }
    }

    private void refresh() {
        if (!baseDirs) {
            return
        }
        initialize(baseDirs)
    }

    private void initialize(File... baseDirs) {
        try {
            if (!baseDirs) {
                return
            }
            this.baseDirs = baseDirs
            if (!ALL_COMPLETERS.contains(this)) {
                ALL_COMPLETERS << this
            }
            SortedSet<String> allStrings = new ConcurrentSkipListSet<>()
            for (File baseDir in baseDirs) {
                def pattern = "file:${baseDir}/**/*.groovy".toString()
                SortedSet<String> strings = RESOURCE_SCAN_CACHE[pattern]
                if (strings == null) {
                    strings = new TreeSet<>()
                    RESOURCE_SCAN_CACHE[pattern] = strings
                    def resources = resourcePatternResolver.getResources(pattern)
                    for (res in resources) {
                        if (isValidResource(res)) {
                            def path = res.file.canonicalPath
                            def basePath = baseDir.canonicalPath
                            path = (path - basePath)[1..-8]
                            path = path.replace(File.separatorChar, '.' as char)
                            strings << path
                        }
                    }
                }
                allStrings.addAll(strings)
            }
            setStrings(allStrings)
        }
        catch (Throwable ignored) {
        }
    }

    boolean isValidResource(Resource resource) {
        true
    }

}
