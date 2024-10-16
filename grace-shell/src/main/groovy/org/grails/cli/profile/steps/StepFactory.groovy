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
package org.grails.cli.profile.steps

import org.grails.cli.profile.Command
import org.grails.cli.profile.Step

/**
 * Creates steps
 *
 * @author Graeme Rocher
 * @since 3.0
 */
interface StepFactory {

    /**
     * Creates a step for the given name, command and parameters
     *
     * @param name The name of the step
     * @param command The command
     * @param parameters The parameters
     *
     * @return The step instance
     */
    Step createStep(String name, Command command, Map parameters)

}
