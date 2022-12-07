package org.grails.plugins;

import org.grails.core.AbstractGrailsClass;

/**
 * Wrapper Grails class for plugins.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsPluginClass extends AbstractGrailsClass {

    public static final String GRAILS_PLUGIN = "GrailsPlugin";

    public GrailsPluginClass(Class<?> clazz) {
        super(clazz, GRAILS_PLUGIN);
    }

}
