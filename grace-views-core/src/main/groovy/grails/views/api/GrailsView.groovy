package grails.views.api

import grails.config.Config
import grails.core.support.proxy.ProxyHandler
import grails.views.GrailsViewTemplate
import grails.views.ResolvableGroovyTemplateEngine
import grails.views.WritableScript
import grails.views.WriterProvider
import grails.web.mapping.LinkGenerator
import grails.web.mime.MimeUtility
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.context.MessageSource

/**
 * A trait for all view types to extend to add methods to generate links, render other templates and so on
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
trait GrailsView extends HttpView implements WriterProvider, WritableScript {

    /**
     * The view template
     */
    GrailsViewTemplate viewTemplate

    /**
     * Whether to pretty print
     */
    boolean prettyPrint = false

    /**
     * The GORM mapping context
     */
    MappingContext getMappingContext() {
        viewTemplate.mappingContext
    }

    /**
     * Handlers for proxies
     */
    ProxyHandler getProxyHandler() {
        viewTemplate.proxyHandler
    }

    /**
     * The link generator
     */
    LinkGenerator getLinkGenerator() {
        viewTemplate.linkGenerator
    }

    /**
     * The mime utility
     */
    MimeUtility getMimeUtility() {
        viewTemplate.mimeUtility
    }

    /**
     * The template engine
     */
    ResolvableGroovyTemplateEngine getTemplateEngine() {
        (ResolvableGroovyTemplateEngine)viewTemplate.templateEngine
    }

    /**
     * The message source object
     */
    MessageSource getMessageSource() {
        viewTemplate.messageSource
    }

    /**
     * @return The current controller namespace
     */
    String controllerNamespace
    /**
     * @return The current controller name
     */
    String controllerName
    /**
     * @return The current action name
     */
    String actionName

    /**
     * @return The configuration
     */
    Config config

    /**
     * Defines the model
     *
     * @param modelDefinition
     */
    void model(Closure modelDefinition) {
        // no-op, added at compile time
    }
}