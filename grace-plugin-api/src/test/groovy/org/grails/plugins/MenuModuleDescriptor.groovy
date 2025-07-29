package org.grails.plugins

import grails.plugins.GrailsPlugin
import grails.plugins.exceptions.PluginException
import grails.plugins.descriptors.AbstractModuleDescriptor

class MenuModuleDescriptor extends AbstractModuleDescriptor {

    String i18n
    String title
    String link
    String location
    int order

    MenuModuleDescriptor() {
        super()
    }

    @Override
    void init(GrailsPlugin plugin, Map args) throws PluginException {
        super.init(plugin, args)
        this.i18n = args.i18n
        this.title = args.title
        this.link = args.link
        this.location = args.location
    }

    @Override
    String toString() {
        StringBuffer sb = new StringBuffer()
        sb.append("MenuModuleDescriptor: [")
                .append("\n    key: ").append(key)
                .append("\n    name: ").append(name)
                .append("\n    description: ").append(description)
                .append("\n    link: ").append(link)
                .append("\n    title: ").append(title)
                .append("\n    location: ").append(location)
                .append("\n    order: ").append(order)
                .append("\n    params: ").append(params)
                .append("]")
        sb.toString()
    }
}
