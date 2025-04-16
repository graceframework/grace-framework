package grails.plugin.component.view

import grails.plugin.component.view.api.ComponentView
import grails.views.Views
import grails.views.WritableScript
import groovy.text.markup.BaseTemplate
import groovy.text.markup.MarkupTemplateEngine
import groovy.text.markup.TemplateConfiguration

/**
 * Base class for markup engine templates
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class ComponentViewTemplate extends BaseTemplate implements WritableScript, ComponentView {

    public static final String EXTENSION = "gcom"
    public static final String TYPE = "views.gcom"

    File sourceFile

    ComponentViewTemplate(MarkupTemplateEngine templateEngine, Map model, Map<String, String> modelTypes, TemplateConfiguration configuration) {
        super(templateEngine, model, modelTypes, configuration)
    }

    @Override
    void setBinding(Binding binding) {
        ((Script)this).setBinding(binding)
    }

    @Override
    Binding getBinding() {
        return ((Script)this).getBinding()
    }
}
