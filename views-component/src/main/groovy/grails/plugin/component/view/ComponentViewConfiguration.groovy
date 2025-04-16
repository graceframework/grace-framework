package grails.plugin.component.view

import grails.views.GenericViewConfiguration
import grails.views.ViewsEnvironment
import grails.web.mime.MimeType
import groovy.text.markup.TemplateConfiguration
import groovy.transform.CompileStatic
import org.springframework.beans.BeanUtils
import org.springframework.boot.context.properties.ConfigurationProperties

import java.beans.PropertyDescriptor

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@ConfigurationProperties('grails.views.component')
class ComponentViewConfiguration extends TemplateConfiguration implements GenericViewConfiguration {

    public static final String MODULE_NAME = "component"

    List<String> mimeTypes = [MimeType.HTML.name, MimeType.XHTML.name]

    ComponentViewConfiguration() {
        setExtension(ComponentViewTemplate.EXTENSION)
        setBaseTemplateClass(ComponentViewTemplate)
        setCacheTemplates( !ViewsEnvironment.isDevelopmentMode() )
        setAutoEscape(true)
        setPrettyPrint( ViewsEnvironment.isDevelopmentMode() )
    }

    @Override
    void setPrettyPrint(boolean prettyPrint) {
        setAutoIndent(true)
        setAutoNewLine(true)
    }

    @Override
    void setEncoding(String encoding) {
        GenericViewConfiguration.super.setEncoding(encoding)
        setDeclarationEncoding(encoding)
    }

    @Override
    boolean isCache() {
        return isCacheTemplates()
    }

    @Override
    void setCache(boolean cache) {
        setCacheTemplates(cache)
    }

    @Override
    String getViewModuleName() {
        MODULE_NAME
    }

    PropertyDescriptor[] findViewConfigPropertyDescriptor() {
        List<PropertyDescriptor> allDescriptors = []
        allDescriptors.addAll(BeanUtils.getPropertyDescriptors(GenericViewConfiguration))
        allDescriptors.addAll(BeanUtils.getPropertyDescriptors(TemplateConfiguration))
        return allDescriptors as PropertyDescriptor[]
    }
}
