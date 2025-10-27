package org.grails.scaffolding.registry;

import org.springframework.beans.factory.InitializingBean;

import grails.web.mapping.LinkGenerator;

import org.grails.scaffolding.registry.input.AssociationInputRenderer;
import org.grails.scaffolding.registry.input.BidirectionalToManyInputRenderer;
import org.grails.scaffolding.registry.input.BooleanInputRenderer;
import org.grails.scaffolding.registry.input.CurrencyInputRenderer;
import org.grails.scaffolding.registry.input.DateInputRenderer;
import org.grails.scaffolding.registry.input.DefaultInputRenderer;
import org.grails.scaffolding.registry.input.EnumInputRenderer;
import org.grails.scaffolding.registry.input.FileInputRenderer;
import org.grails.scaffolding.registry.input.InListInputRenderer;
import org.grails.scaffolding.registry.input.LocaleInputRenderer;
import org.grails.scaffolding.registry.input.NumberInputRenderer;
import org.grails.scaffolding.registry.input.StringInputRenderer;
import org.grails.scaffolding.registry.input.TextareaInputRenderer;
import org.grails.scaffolding.registry.input.TimeInputRenderer;
import org.grails.scaffolding.registry.input.TimeZoneInputRenderer;
import org.grails.scaffolding.registry.input.UrlInputRenderer;
import org.grails.scaffolding.registry.output.DefaultOutputRenderer;

/**
 * Bean for registering the default domain renderers
 *
 * @author James Kleeh
 */
public class DomainRendererRegisterer implements InitializingBean {

    DomainInputRendererRegistry domainInputRendererRegistry;

    DomainOutputRendererRegistry domainOutputRendererRegistry;

    LinkGenerator grailsLinkGenerator;

    public DomainRendererRegisterer(DomainInputRendererRegistry domainInputRendererRegistry,
            DomainOutputRendererRegistry domainOutputRendererRegistry) {
        this(domainInputRendererRegistry, domainOutputRendererRegistry, null);
    }

    public DomainRendererRegisterer(DomainInputRendererRegistry domainInputRendererRegistry, DomainOutputRendererRegistry domainOutputRendererRegistry,
            LinkGenerator grailsLinkGenerator) {
        this.domainInputRendererRegistry = domainInputRendererRegistry;
        this.domainOutputRendererRegistry = domainOutputRendererRegistry;
        this.grailsLinkGenerator = grailsLinkGenerator;
    }

    @Override
    public void afterPropertiesSet() {
        this.domainInputRendererRegistry.registerDomainRenderer(new DefaultInputRenderer(), -3);
        this.domainInputRendererRegistry.registerDomainRenderer(new UrlInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new TimeZoneInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new TimeInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new StringInputRenderer(), -2);
        this.domainInputRendererRegistry.registerDomainRenderer(new TextareaInputRenderer(), -2);
        this.domainInputRendererRegistry.registerDomainRenderer(new NumberInputRenderer(), -2);
        this.domainInputRendererRegistry.registerDomainRenderer(new LocaleInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new InListInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new FileInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new EnumInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new DateInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new CurrencyInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new BooleanInputRenderer(), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new BidirectionalToManyInputRenderer(this.grailsLinkGenerator), -1);
        this.domainInputRendererRegistry.registerDomainRenderer(new AssociationInputRenderer(), -2);

        this.domainOutputRendererRegistry.registerDomainRenderer(new DefaultOutputRenderer(), -1);
    }

    public void registerRenderers() {
        this.afterPropertiesSet();
    }

}
