/*
 * Copyright 2014-2023 the original author or authors.
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
package grails.dev.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grails.core.io.support.GrailsFactoriesLoader;

/**
 * A registry of {@link ApplicationCommand} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public final class ApplicationContextCommandRegistry {

    private static final Map<String, ApplicationCommand> APPLICATION_COMMANDS = new HashMap<>();

    static {
        List<ApplicationCommand> registeredCommands = GrailsFactoriesLoader.loadFactories(ApplicationCommand.class);

        for (ApplicationCommand cmd : registeredCommands) {
            APPLICATION_COMMANDS.put(cmd.getName(), cmd);
        }
    }

    private ApplicationContextCommandRegistry() {
    }

    public static Collection<ApplicationCommand> findCommands() {
        return APPLICATION_COMMANDS.values();
    }

    public static ApplicationCommand findCommand(String name) {
        return APPLICATION_COMMANDS.get(name);
    }

}
