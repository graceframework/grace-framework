/*
 * Copyright 2011-2023 the original author or authors.
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

import java.io.IOException;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import jline.console.completer.CompletionHandler;

/**
 * Fixes issues with the default CandidateListCompletionHandler such as clearing out the whole buffer when
 * a completion matches a list of candidates
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CandidateListCompletionHandler implements CompletionHandler {

    private boolean eagerNewlines = true;

    public void setAlwaysIncludeNewline(boolean eagerNewlines) {
        this.eagerNewlines = eagerNewlines;
    }

    public boolean complete(ConsoleReader reader, List<CharSequence> candidates, int pos) throws IOException {
        CursorBuffer buf = reader.getCursorBuffer();

        // if there is only one completion, then fill in the buffer
        if (candidates.size() == 1) {
            String value = candidates.get(0).toString();

            // fail if the only candidate is the same as the current buffer
            if (value.equals(buf.toString())) {
                return false;
            }

            jline.console.completer.CandidateListCompletionHandler.setBuffer(reader, value, pos);

            return true;
        }

        if (candidates.size() > 1) {
            String value = getUnambiguousCompletions(candidates);

            jline.console.completer.CandidateListCompletionHandler.setBuffer(reader, value, pos);
        }

        if (this.eagerNewlines) {
            reader.println();
        }
        jline.console.completer.CandidateListCompletionHandler.printCandidates(reader, candidates);

        // redraw the current console buffer
        reader.drawLine();

        return true;
    }

    /**
     * Returns a root that matches all the {@link String} elements
     * of the specified {@link List}, or null if there are
     * no commalities. For example, if the list contains
     * <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the
     * method will return <i>foob</i>.
     */
    private String getUnambiguousCompletions(final List<?> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // convert to an array for speed
        String[] strings = candidates.toArray(new String[0]);

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
        for (String candidate : candidates) {
            if (!candidate.startsWith(starts)) {
                return false;
            }
        }

        return true;
    }
}
