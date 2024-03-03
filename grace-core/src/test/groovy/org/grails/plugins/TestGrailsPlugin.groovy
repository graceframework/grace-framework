package org.grails.plugins

import grails.plugins.Plugin

class TestGrailsPlugin extends Plugin {
    def version = '1.0'
    def grailsVersion = '4.0.1'
    def loadAfter = ['testTwo']
}
