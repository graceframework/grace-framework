package org.grails.scaffolding.registry;

import java.util.Map;

import groovy.lang.Closure;
import groovy.xml.MarkupBuilder;

import org.grails.scaffolding.model.property.DomainProperty;

/**
 * Used to render a single domain class property on a form
 *
 * @author James Kleeh
 */
public interface DomainInputRenderer extends DomainRenderer {

    /**
     * Defines how a given domain class property will be rendered in the context of a form
     *
     * @param defaultAttributes The default html element attributes
     * @param property          The domain property to be rendered
     * @return The closure to be passed to an instance of {@link MarkupBuilder}
     */
    Closure renderInput(Map defaultAttributes, DomainProperty property);

}
