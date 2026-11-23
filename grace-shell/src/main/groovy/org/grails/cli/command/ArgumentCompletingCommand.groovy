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
package org.grails.cli.command

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser

/**
 * @author graemerocher
 * @author Michael Yan
 * @since 3.0
 */
abstract class ArgumentCompletingCommand implements Command, Completer {

    CommandLineParser cliParser = new CommandLineParser()

    @Override
    void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        CommandDescription desc = getDescription()
        CommandLine commandLine = cliParser.parseString(line.line())
        complete(commandLine, desc, candidates)
    }

    protected void complete(CommandLine commandLine, CommandDescription desc, List<Candidate> candidates) {
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
                        candidates.add(new Candidate("${argName.substring(lastArg.length())} ".toString()))
                    }
                    else if (!invalidOptions) {
                        candidates.add(new Candidate("$flag ".toString()))
                    }
                }
                else {
                    candidates.add(new Candidate("$flag ".toString()))
                }
            }
        }
    }

}
