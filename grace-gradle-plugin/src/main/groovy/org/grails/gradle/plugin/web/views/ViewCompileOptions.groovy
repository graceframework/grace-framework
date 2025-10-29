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
package org.grails.gradle.plugin.web.views

import javax.inject.Inject

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.compile.AbstractOptions
import org.gradle.api.tasks.compile.GroovyForkOptions

/**
 * @author Graeme Rocher
 * @since 2024.0.0
 */
class ViewCompileOptions extends AbstractOptions {

    @Input
    String encoding = 'UTF-8'

    @Nested
    GroovyForkOptions forkOptions = getObjectFactory().newInstance(GroovyForkOptions)

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException()
    }

}
