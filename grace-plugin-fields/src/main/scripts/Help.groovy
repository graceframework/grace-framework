import grails.plugin.formfields.FieldsGrailsPlugin

namespace 'fields'
description "Print Help infromation for Fields", "grace fields:help"
visible false

URL url = FieldsGrailsPlugin.getResource('/META-INF/HELP')

consoleLogger.addStatus "HELP"

consoleLogger.log url.text