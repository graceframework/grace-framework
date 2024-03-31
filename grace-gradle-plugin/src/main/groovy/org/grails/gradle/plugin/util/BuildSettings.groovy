/*
 * Copyright 2022-2023 the original author or authors.
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

/**
 * Build time settings and configuration
 *
 * GrailsGradlePlugin use this BuildSettings to avoid pre init {@link grails.util.BuildSettings}
 *
 * @author Michael Yan
 * @since 2022.1.7
 */
@CompileStatic
class BuildSettings {

    /**
     * The base directory of the application
     */
    public static final String APP_BASE_DIR = 'base.dir'

    /**
     * The app directory of the application
     */
    public static final String APP_DIR = 'grails.app.dir'

    /**
     * The name of the system property for the the project target directory.
     * Must be set if Gradle build location is changed.
     */
    public static final String PROJECT_TARGET_DIR = 'grails.project.target.dir'

    /**
     * The name of the system property for {@link #}.
     */
    public static final String PROJECT_RESOURCES_DIR = 'grails.project.resource.dir'

    /**
     * The name of the system property for the project classes directory.
     */
    public static final String PROJECT_CLASSES_DIR = 'grails.project.class.dir'

}
