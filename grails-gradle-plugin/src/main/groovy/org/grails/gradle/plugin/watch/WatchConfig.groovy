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
package org.grails.gradle.plugin.watch

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.Named

/**
 *
 * A configuration for watching files for changes
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@ToString
class WatchConfig implements Named {

    WatchConfig(String name) {
        this.name = name
    }

    /**
     * The name of the config
     */
    String name
    /**
     * The directory to watch
     */
    File directory

    /**
     * The file extensions to watch
     */
    List<String> extensions

    /**
     * The tasks to execute
     */
    List<String> tasks = []

    /**
     * The tasks to trigger when a modification event is received
     *
     * @param tasks The tasks
     */
    void tasks(String... tasks) {
        this.tasks.addAll(Arrays.asList(tasks))
    }

}
