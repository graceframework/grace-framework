package org.grails.scaffolding.registry;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.grails.scaffolding.model.property.DomainProperty;

/**
 * A registry of domain property renderers sorted by priority and order of addition
 *
 * @author James Kleeh
 */
public abstract class DomainRendererRegistry<T extends DomainRenderer> {

    protected SortedSet<Entry> domainRegistryEntries = new TreeSet<>();

    protected final AtomicInteger RENDERER_SEQUENCE = new AtomicInteger(0);

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
