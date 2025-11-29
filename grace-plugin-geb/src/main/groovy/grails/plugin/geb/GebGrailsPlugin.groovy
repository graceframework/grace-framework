package grails.plugin.geb

import groovy.transform.CompileStatic

import grails.plugins.Plugin
import grails.util.GrailsUtil

@CompileStatic
class GebGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def grailsVersion = '2023.0.0 > *'
    def title = 'Grace Geb Plugin'
    def description = 'Plugin that adds Geb functional testing code generation features.'

}
