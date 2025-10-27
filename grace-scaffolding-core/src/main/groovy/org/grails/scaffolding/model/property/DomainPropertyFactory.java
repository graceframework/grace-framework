package org.grails.scaffolding.model.property;

import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Embedded;

/**
 * A factory to create instances of {@link DomainProperty}
 *
 * @author James Kleeh
 */
public interface DomainPropertyFactory {

    /**
     * @param persistentProperty The persistent property
     * @return The {@link DomainProperty} representing the {@link PersistentProperty}
     */
    DomainProperty build(PersistentProperty persistentProperty);

    /**
     * @param rootProperty       The root property.
     *                           Typically, an instance of {@link Embedded}
     * @param persistentProperty The persistent property
     * @return The {@link DomainProperty} representing the {@link PersistentProperty}
     */
    DomainProperty build(PersistentProperty rootProperty, PersistentProperty persistentProperty);

}
