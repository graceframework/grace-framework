/*
 * Copyright 2015-2022 the original author or authors.
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
package org.grails.cli.interactive.completers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jline.console.completer.Completer;
import jline.internal.Preconditions;

/**
 * Copied from jline AggregateCompleter
 *
 * sorts aggregated completions
 *
 */
public class SortedAggregateCompleter implements Completer {

    private final List<Completer> completers = new ArrayList<>();

    public SortedAggregateCompleter() {
        // empty
    }

    /**
     * Construct an AggregateCompleter with the given collection of completers.
     * The completers will be used in the iteration order of the collection.
     *
     * @param completers the collection of completers
     */
    public SortedAggregateCompleter(final Collection<Completer> completers) {
        Preconditions.checkNotNull(completers);
        this.completers.addAll(completers);
    }

    /**
     * Construct an AggregateCompleter with the given completers.
     * The completers will be used in the order given.
     *
     * @param completers the completers
     */
    public SortedAggregateCompleter(final Completer... completers) {
        this(Arrays.asList(completers));
    }

    /**
     * Retrieve the collection of completers currently being aggregated.
     *
     * @return the aggregated completers
     */
    public Collection<Completer> getCompleters() {
        return this.completers;
    }

    /**
     * Perform a completion operation across all aggregated completers.
     *
     * @see Completer#complete(String, int, java.util.List)
     * @return the highest completion return value from all completers
     */
    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
        // buffer could be null
        Preconditions.checkNotNull(candidates);

        List<Completion> completions = new ArrayList<>(this.completers.size());

        // Run each completer, saving its completion results
        int max = -1;
        for (Completer completer : this.completers) {
            Completion completion = new Completion(candidates);
            completion.complete(completer, buffer, cursor);

            // Compute the max cursor position
            max = Math.max(max, completion.cursor);

            completions.add(completion);
        }

        SortedSet<CharSequence> allCandidates = new TreeSet<>();

        // Append candidates from completions which have the same cursor position as max
        for (Completion completion : completions) {
            if (completion.cursor == max) {
                allCandidates.addAll(completion.candidates);
            }
        }

        candidates.addAll(allCandidates);

        return max;
    }

    /**
     * @return a string representing the aggregated completers
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "completers=" + this.completers +
                '}';
    }

    private class Completion {

        public final List<CharSequence> candidates;

        public int cursor;

        public Completion(final List<CharSequence> candidates) {
            Preconditions.checkNotNull(candidates);
            this.candidates = new LinkedList<>(candidates);
        }

        public void complete(final Completer completer, final String buffer, final int cursor) {
            Preconditions.checkNotNull(completer);
            this.cursor = completer.complete(buffer, cursor, this.candidates);
        }

    }

}
