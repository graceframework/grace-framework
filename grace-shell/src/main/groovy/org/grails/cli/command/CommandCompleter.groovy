/*
 * Copyright 2015-2026 the original author or authors.
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

import groovy.transform.CompileStatic
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * A completer for commands
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
class CommandCompleter implements Completer {

    Collection<Command> commands

    CommandCompleter(Collection<Command> commands) {
        this.commands = commands
    }

    @Override
    void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line()
        Command cmd = commands.find {
            String trimmed = buffer.trim()
            if (trimmed.split(/\s/).size() > 1) {
                return trimmed.startsWith(it.name)
            }

            trimmed == it.name
        }
        if (cmd instanceof Completer) {
            ((Completer) cmd).complete(reader, line, candidates)
        }
    }

}
