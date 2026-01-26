/*
 * Copyright 2022-2026 the original author or authors.
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
package grails.cli.command;

import grails.util.Described;
import grails.util.GrailsNameUtils;
import grails.util.Named;

/**
 * The Command interface supports default name, description, and group
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
public interface Command extends Named, Described {

    String DEFAULT_COMMAND_GROUP = "Command";

    @Override
    default String getName() {
        String name = GrailsNameUtils.getLogicalName(getClass().getName(), "Command");
        return GrailsNameUtils.getScriptName(name);
    }

    @Override
    default String getDescription() {
        return getName();
    }

    default String getGroup() {
        return DEFAULT_COMMAND_GROUP;
    }

    boolean handle(ExecutionContext executionContext);

}
