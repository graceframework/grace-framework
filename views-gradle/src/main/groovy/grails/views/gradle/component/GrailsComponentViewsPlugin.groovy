package grails.views.gradle.component

import groovy.transform.CompileStatic

import grails.views.gradle.AbstractGroovyTemplatePlugin

/**
 * A plugin for compiling markup templates
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class GrailsComponentViewsPlugin extends AbstractGroovyTemplatePlugin {

    GrailsComponentViewsPlugin() {
        super(ComponentViewCompilerTask, "gcom")
    }
}
