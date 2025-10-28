package org.grails.gradle.plugin.web.views.json

import org.grails.gradle.plugin.web.views.AbstractGroovyTemplatePlugin
import org.grails.gradle.plugin.web.views.markup.MarkupViewCompilerTask
import groovy.transform.CompileStatic

/**
 * A plugin for compiling markup templates
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
@CompileStatic
class GrailsMarkupViewsPlugin extends AbstractGroovyTemplatePlugin {

    GrailsMarkupViewsPlugin() {
        super(MarkupViewCompilerTask, "gml")
    }
}
