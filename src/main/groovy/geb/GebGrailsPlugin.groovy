package geb

import grails.plugins.Plugin
import groovy.transform.CompileStatic

@CompileStatic
class GebGrailsPlugin extends Plugin {

    def grailsVersion = "2023.0.0 > *"
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]
    def title = "Grace Geb Plugin"
    def author = "Michael Yan"
    def authorEmail = "rain@rainboyan.com"
    def description = '''\
Plugin that adds Geb functional testing code generation features.
'''
    def documentation = "https://github.com/graceframework/grace-geb"
    def license = "APACHE"
    def issueManagement = [system: "Github Issues", url: "https://github.com/graceframework/grace-geb"]
    def scm = [url: "https://github.com/graceframework/grace-geb"]

}
