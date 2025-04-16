package grails.plugin.component.view.renderer

import grails.core.support.proxy.ProxyHandler
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.views.mvc.SmartViewResolver
import grails.views.mvc.renderer.DefaultViewRenderer
import grails.web.mime.MimeType

/**
 * Integration with the Grails renderer framework
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 */
class ComponentViewHtmlRenderer<T> extends DefaultViewRenderer<T> {
    ComponentViewHtmlRenderer(Class<T> targetType, SmartViewResolver viewResolver, ProxyHandler proxyHandler, RendererRegistry rendererRegistry, Renderer defaultRenderer) {
        super(targetType, MimeType.HTML, viewResolver, proxyHandler, rendererRegistry, defaultRenderer)
    }

    ComponentViewHtmlRenderer(Class<T> targetType, MimeType mimeType, SmartViewResolver viewResolver, ProxyHandler proxyHandler, RendererRegistry rendererRegistry, Renderer defaultRenderer) {
        super(targetType, mimeType, viewResolver, proxyHandler, rendererRegistry, defaultRenderer)
    }
}
