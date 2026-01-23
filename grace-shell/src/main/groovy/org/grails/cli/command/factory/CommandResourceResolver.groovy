/*
 * Copyright 2014-2026 the original author or authors.
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
package org.grails.cli.command.factory

import org.springframework.core.io.Resource

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
interface CommandResourceResolver {

    /**
     * Finds {@link org.grails.cli.command.Command} resources
     *
     * @return A collection of {@link Resource} instances
     */
    Collection<Resource> findCommandResources()

    /**
     * The pattern to match file names with
     *
     * @return A regex pattern
     */
    Collection<String> getMatchingFileExtensions()

}
