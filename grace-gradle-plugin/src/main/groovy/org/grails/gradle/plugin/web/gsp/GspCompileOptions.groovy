/*
 * Copyright 2020-2022 the original author or authors.
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
package org.grails.gradle.plugin.web.gsp

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.compile.GroovyForkOptions

/**
 * Presents the Compile Options used by the {@llink GroovyPageForkCompileTask}
 *
 * @author David Estes
 * @since 4.0
 */
class GspCompileOptions implements Serializable {

    private static final long serialVersionUID = 0L

    @Input
    String encoding = 'UTF-8'

    @Nested
    GroovyForkOptions forkOptions = new GroovyForkOptions()

}