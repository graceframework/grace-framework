/*
 * Copyright 2022-2024 the original author or authors.
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
package org.grails.plugins.modules

import grails.plugins.Plugin
import grails.util.GrailsUtil

/**
 * @since 2022.1.0
 * @deprecated since 2023.0.0, in favor of org.graceframework.plugins:dynamic-modules
 */
@Deprecated(since = "2023.0.0")
class DynamicModulesGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def title = 'Grails Dynamic Modules Plugin'
    def author = 'Michael Yan'
    def authorEmail = 'rain@rainboyan.com'
    def description = '''\
Grails Dynamic Modules Plugin offer new ways of creating modular and maintainable Grails applications.
'''

    Closure doWithSpring() {
        { ->
        }
    }

}
