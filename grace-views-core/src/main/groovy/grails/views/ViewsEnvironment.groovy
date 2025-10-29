/*
 * Copyright 2015-2025 the original author or authors.
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
package grails.views

import grails.util.BuildSettings
import grails.util.Environment

/**
 * Environment helper methods
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
class ViewsEnvironment {

    private static final boolean DEVELOPMENT_MODE = Environment.getCurrent() == Environment.DEVELOPMENT && BuildSettings.GRAILS_APP_DIR_PRESENT

    /**
     * @return Whether development mode is enabled
     */
    static boolean isDevelopmentMode() {
        DEVELOPMENT_MODE
    }

    static String findTemplatePath() {
        File viewDir = new File(BuildSettings.GRAILS_APP_DIR, 'views')
        return viewDir
    }

}
