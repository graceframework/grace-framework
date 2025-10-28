package org.grails.gradle.plugin.web.views.json

import org.grails.gradle.plugin.web.views.AbstractGroovyTemplatePlugin
import groovy.transform.CompileStatic

/**
 * Concrete implementation of plugin for JSON views
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
@CompileStatic
class GrailsJsonViewsPlugin extends AbstractGroovyTemplatePlugin {

    GrailsJsonViewsPlugin() {
        super(JsonViewCompilerTask, "gson")
    }
}

