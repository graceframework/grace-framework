/*
 * Copyright 2011-2026 the original author or authors.
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
package org.grails.cli.command.completers

import groovy.transform.CompileStatic
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * JLine Completor that mixes a fixed set of options with file path matches.
 * Fixed options that match will appear first, followed by file path matches.
 *
 * @author Peter Ledbrook
 * @author Michael Yan
 * @since 2.0
 */
@CompileStatic
class SimpleOrFileNameCompletor implements Completer {

    private final StringsCompleter simpleCompletor
    private final EscapingFileNameCompletor fileNameCompletor

    SimpleOrFileNameCompletor(List fixedOptions) {
        this(fixedOptions as String[])
    }
    SimpleOrFileNameCompletor(String[] fixedOptions) {
        this.simpleCompletor = new StringsCompleter(fixedOptions)
        this.fileNameCompletor = new EscapingFileNameCompletor()
    }

    @Override
    void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Try the simple completor first...
        List<Candidate> simpleCandidates = new ArrayList<>()
        this.simpleCompletor.complete(reader, line, simpleCandidates)
        candidates.addAll(simpleCandidates)

        // ...and then the file path completor. By using the given candidate
        // list with both completors we aggregate the results automatically.
        List<Candidate> fileCandidates = new ArrayList<>()
        this.fileNameCompletor.complete(reader, line, fileCandidates)
        candidates.addAll(fileCandidates)
    }

}
