package grails.plugin.scaffolding

import grails.plugins.*

class ScaffoldingGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2023.0.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "Scaffolding Plugin" // Headline display name of the plugin
    def author = "Michael Yan"
    def authorEmail = "rain@rainboyan.com"
    def description = '''\
Plugin that generates scaffolded controllers and views for a Grace application.
'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/graceframework/grace-scaffolding"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "Github", url: "https://github.com/graceframework/grace-scaffolding/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/graceframework/grace-scaffolding" ]

}
