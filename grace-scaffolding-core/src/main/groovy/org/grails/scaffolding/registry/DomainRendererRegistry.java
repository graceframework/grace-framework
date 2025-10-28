/*
 * Copyright 2012-2025 the original author or authors.
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
package org.grails.scaffolding.registry;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.grails.scaffolding.model.property.DomainProperty;

/**
 * A registry of domain property renderers sorted by priority and order of addition
 *
 * @author James Kleeh
 * @since 2024.0.0
 */
public abstract class DomainRendererRegistry<T extends DomainRenderer> {

    protected SortedSet<Entry> domainRegistryEntries = new TreeSet<>();

    protected static final AtomicInteger RENDERER_SEQUENCE = new AtomicInteger(0);

    void registerDomainRenderer(T domainRenderer, Integer priority) {
        this.domainRegistryEntries.add(new Entry(domainRenderer, priority));
    }

    public SortedSet<Entry> getDomainRegistryEntries() {
        return this.domainRegistryEntries;
    }

    public T get(DomainProperty domainProperty) {
        for (Entry entry : this.domainRegistryEntries) {
            if (entry.renderer.supports(domainProperty)) {
                return entry.renderer;
            }
        }
        return null;
    }

    private class Entry implements Comparable<Entry> {

        protected final T renderer;

        private final int priority;

        private final int seq;

        Entry(T renderer, int priority) {
            this.renderer = renderer;
            this.priority = priority;
            this.seq = RENDERER_SEQUENCE.incrementAndGet();
        }

        public int compareTo(Entry entry) {
            return this.priority == entry.priority ? entry.seq - this.seq : entry.priority - this.priority;
        }

    }

}
