package grails.boot

import grails.boot.annotation.GrailsComponentScan
import grails.plugins.Plugin

@GrailsComponentScan
class WatchedResourcesGrailsPlugin extends Plugin {
    def version = "1.0"
    def watchedResources = "file:./**/*.properties"

    void onChange(Map<String, Object> event) {
        fileIsChanged = event.source.path.toString()
    }
    String fileIsChanged = ""
}
