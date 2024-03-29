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
package org.grails.cli.profile.commands

import jline.console.completer.Completer

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.grails.cli.profile.Command
import org.grails.cli.profile.CommandDescription

/**
 * @author graemerocher
 */
abstract class ArgumentCompletingCommand implements Command, Completer {

    CommandLineParser cliParser = new CommandLineParser()

    @Override
    int complete(String buffer, int cursor, List<CharSequence> candidates) {
        CommandDescription desc = getDescription()
        CommandLine commandLine = cliParser.parseString(buffer)
        complete(commandLine, desc, candidates, cursor)
    }

    protected int complete(CommandLine commandLine, CommandDescription desc, List<CharSequence> candidates, int cursor) {
        Set<String> invalidOptions = commandLine.undeclaredOptions.keySet().findAll { String str ->
            desc.getFlag(str.trim()) == null
        }

        Map.Entry<String, Object> lastOption = commandLine.lastOption()

        for (arg in desc.flags) {
            String argName = arg.name
            String flag = "-$argName"
            if (!commandLine.hasOption(arg.name)) {
                if (lastOption) {
                    String lastArg = lastOption.key
                    if (arg.name.startsWith(lastArg)) {
                        candidates.add("${argName.substring(lastArg.length())} ".toString())
                    }
                    else if (!invalidOptions) {
                        candidates.add "$flag ".toString()
                    }
                }
                else {
                    candidates.add "$flag ".toString()
                }
            }
        }
        cursor
    }

}
