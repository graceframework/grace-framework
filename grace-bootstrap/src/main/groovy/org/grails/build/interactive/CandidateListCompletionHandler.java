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
package org.grails.build.interactive;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * Fixes issues with the default CandidateListCompletionHandler such as clearing out the whole buffer when
 * a completion matches a list of candidates
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
public class CandidateListCompletionHandler implements Completer {

    private final Completer delegate;

    public CandidateListCompletionHandler(Completer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (this.delegate != null) {
            this.delegate.complete(reader, line, candidates);
        }
    }

    /**
     * Returns a root that matches all the {@link String} elements
     * of the specified {@link List}, or null if there are
     * no commalities. For example, if the list contains
     * <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the
     * method will return <i>foob</i>.
     */
    private String getUnambiguousCompletions(final List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // convert to an array for speed
        String[] strings = candidates.toArray(new String[0]);
        for (int i = 0; i < candidates.size(); i++) {
            strings[i] = candidates.get(i).value();
        }

        String first = strings[0];
        StringBuilder candidate = new StringBuilder();

        for (int i = 0, count = first.length(); i < count; i++) {
            if (startsWith(first.substring(0, i + 1), strings)) {
                candidate.append(first.charAt(i));
            }
            else {
                break;
            }
        }

        return candidate.toString();
    }

    /**
     * @return true is all the elements of <i>candidates</i>
     *         start with <i>starts</i>
     */
    private boolean startsWith(String starts, String[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (!candidates[i].startsWith(starts)) {
                return false;
            }
        }

        return true;
    }

}
