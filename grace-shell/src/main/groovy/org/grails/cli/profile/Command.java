/*
 * Copyright 2014-2024 the original author or authors.
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
package org.grails.cli.profile;

import grails.util.Named;

/**
 * An interface that represents a command to be executed by the Grails command line. Commands are by default global,
 * however a command can be made specific to a particular {@link Profile} by implementation the {@link ProfileCommand} interface.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
public interface Command extends Named {

    /**
     * @return The description of the command
     */
    CommandDescription getDescription();

    /**
     * run the command
     *
     * @param executionContext The {@link ExecutionContext}
     *
     * @return Whether the command should continue
     */
    boolean handle(ExecutionContext executionContext);

    /**
     * Return the namespace of the command.
     *
     * @return empty if the command don't have a namespace.
     */
    default String getNamespace() {
        return "";
    }

    /**
     * Return the full name of a command.
     * If the command have a namespace, the full name will be {@code namespace:name}.
     *
     * @return the full name of a command.
     */
    default String getFullName() {
        if (getNamespace() != null && !getNamespace().isBlank()) {
            return String.format("%s:%s", getNamespace(), getName());
        }
        else {
            return getName();
        }
    }

    /**
     * Returns true if this is a visible command. A visible command will be listed in {@code 'help'} command.
     * The default value is {@code true}.
     *
     * @return true if this is a visible command.
     */
    default boolean isVisible() {
        return true;
    }

}
