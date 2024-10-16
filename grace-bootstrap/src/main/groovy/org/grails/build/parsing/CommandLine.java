/*
 * Copyright 2011-2024 the original author or authors.
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
package org.grails.build.parsing;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Represents the parsed command line options.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
public interface CommandLine {

    String DEBUG_FORK = "debug-fork";

    String OFFLINE_ARGUMENT = "offline";

    String VERBOSE_ARGUMENT = "verbose";

    String STACKTRACE_ARGUMENT = "stacktrace";

    String QUIET_ARGUMENT = "quiet";

    String AGENT_ARGUMENT = "reloading";

    String VERSION_ARGUMENT = "version";

    String REFRESH_DEPENDENCIES_ARGUMENT = "refresh-dependencies";

    String HELP_ARGUMENT = "help";

    String NOANSI_ARGUMENT = "plain-output";

    String NON_INTERACTIVE_ARGUMENT = "non-interactive";

    /**
     * @return The environment specified
     */
    String getEnvironment();

    /**
     * @return Whether the environment is user specified
     */
    boolean isEnvironmentSet();

    /**
     * @return The command name specified
     */
    String getCommandName();

    /**
     * @return The remaining arguments after the command name
     */
    List<String> getRemainingArgs();

    /**
     * @return The remaining arguments as an array
     */
    String[] getRemainingArgsArray();

    /**
     * @return The system properties specified
     */
    Properties getSystemProperties();

    /**
     * @param name The name of the option
     * @return Whether the given option is specified
     */
    boolean hasOption(String name);

    /**
     * The value of an option
     * @param name The option
     * @return The value
     */
    Object optionValue(String name);

    /**
     * @return The last specified option
     */
    Map.Entry<String, Object> lastOption();

    /**
     * @return The remaining args as one big string
     */
    String getRemainingArgsString();

    /**
     * @return The remaining args as one big string without undeclared options
     */
    String getRemainingArgsWithOptionsString();

    /**
     * @return The remaining args separated by the line separator char
     */
    String getRemainingArgsLineSeparated();

    Map<String, Object> getDeclaredOptions();

    Map<String, Object> getUndeclaredOptions();

    String lookupEnvironmentForCommand();

    void setCommand(String scriptName);

    /**
     * Parses a new {@link CommandLine} instance that combines this instance with the given arguments
     * @param args The arguments
     * @return A new {@link CommandLine} instance
     */
    CommandLine parseNew(String[] args);

    String[] getRawArguments();

}
