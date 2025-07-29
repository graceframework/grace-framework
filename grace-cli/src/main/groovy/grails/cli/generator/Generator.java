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
package grails.cli.generator;

import grails.util.GrailsNameUtils;

/**
 * @author Michael Yan
 * @since 2023.2.0
 */
public interface Generator {

    default String getName() {
        return GrailsNameUtils.getLogicalPropertyName(getClass().getName(), "Generator");
    }

    default String getDescription() {
        return getName();
    }

    default void init(GenerationContext generationContext) {
    }

    default boolean generate() {
        return true;
    }

    default boolean help() {
        return true;
    }

    default boolean revoke() {
        return true;
    }

}
