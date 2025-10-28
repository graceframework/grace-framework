package grails.plugin.json.view.template

import grails.plugin.json.builder.JsonGenerator
import grails.plugin.json.view.JsonViewWritableScript
import grails.plugin.json.view.api.jsonapi.JsonApiIdRenderStrategy
import grails.views.GrailsViewTemplate
import groovy.transform.CompileStatic

import grails.views.api.GrailsView

@CompileStatic
class JsonViewTemplate extends GrailsViewTemplate {

    JsonGenerator generator
    JsonApiIdRenderStrategy jsonApiIdRenderStrategy

    JsonViewTemplate(Class<? extends GrailsView> templateClass) {
        super(templateClass)
    }

    JsonViewTemplate(Class<? extends GrailsView> templateClass, File sourceFile) {
        super(templateClass, sourceFile)
    }

    @Override
    Writable make(Map binding) {
        JsonViewWritableScript writableTemplate = (JsonViewWritableScript)super.make(binding)
        writableTemplate.setGenerator(generator)
        writableTemplate.setJsonApiIdRenderStrategy(jsonApiIdRenderStrategy)
        writableTemplate
    }
}
