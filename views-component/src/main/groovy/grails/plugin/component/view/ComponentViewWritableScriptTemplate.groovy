package grails.plugin.component.view

import grails.plugin.component.view.api.ComponentView
import grails.views.GrailsViewTemplate
import grails.views.api.GrailsView
import groovy.text.markup.MarkupTemplateEngine
import groovy.transform.CompileStatic
/**
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class ComponentViewWritableScriptTemplate extends GrailsViewTemplate {

    MarkupTemplateEngine templateEngine
    ComponentViewConfiguration configuration

    ComponentViewWritableScriptTemplate(Class<? extends GrailsView> templateClass, File sourceFile, MarkupTemplateEngine templateEngine, ComponentViewConfiguration configuration) {
        super(templateClass, sourceFile)
        this.templateEngine = templateEngine
        this.configuration = configuration
    }

    @Override
    Writable make(Map binding) {
        ComponentView writableTemplate = (ComponentView)templateClass
                .newInstance(templateEngine, binding, Collections.emptyMap(), configuration)
        writableTemplate.viewTemplate = (GrailsViewTemplate)this
        writableTemplate.prettyPrint = prettyPrint

        writableTemplate.setSourceFile(sourceFile)

        return writableTemplate
    }
}
