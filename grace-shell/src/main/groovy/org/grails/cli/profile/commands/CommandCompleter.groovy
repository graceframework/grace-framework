/*
 * Copyright 2015-2023 the original author or authors.
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

import org.grails.cli.profile.Command

/**
 * A completer for commands
 *
 * @author Graeme Rocher
 * @since 3.1
 */
class CommandCompleter implements Completer {

    Collection<Command> commands

    CommandCompleter(Collection<Command> commands) {
        this.commands = commands
    }

    @Override
    int complete(String buffer, int cursor, List<CharSequence> candidates) {
        Command cmd = commands.find {
            String trimmed = buffer.trim()
            if (trimmed.split(/\s/).size() > 1) {
                return trimmed.startsWith(it.name)
            }

            trimmed == it.name
        }
        if (cmd instanceof Completer) {
            return ((Completer) cmd).complete(buffer, cursor, candidates)
        }
        cursor
    }

}
