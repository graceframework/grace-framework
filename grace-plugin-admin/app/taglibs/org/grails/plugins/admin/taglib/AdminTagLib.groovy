/*
 * Copyright 2022-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins.admin.taglib

import org.apache.commons.lang3.StringUtils
import com.opensymphony.module.sitemesh.RequestConstants

/**
 * AdminTagLib
 *
 * @author Michael Yan
 * @since 0.0.1
 */
class AdminTagLib {

    static namespace = "admin"
    static final String SYSTEM_ADMIN_NAVIGATION_BAR = "admin.navigation.bar"

    def webInterfaceManager

    def navbar = { attrs, body ->
        def page = request.getAttribute(RequestConstants.PAGE)
        def currentSection = page.getProperty("meta.admin.active.section")
        def currentItem = page.getProperty("meta.admin.active.item")

        currentSection = StringUtils.stripToNull(currentSection)
        def selectedMenuSection = currentSection
        if(StringUtils.contains(currentSection, "/")) {
            selectedMenuSection = currentSection.substring(0, currentSection.indexOf("/"))
        }
        def bean = attrs.remove('bean')
        def justlinks = attrs.remove('justlinks')

        def sections = webInterfaceManager.getDisplayableSections(SYSTEM_ADMIN_NAVIGATION_BAR, [:])
        if (sections) {
            StringBuilder buf = new StringBuilder()
            if (!justlinks) buf.append("<ul class=\"navbar-nav mr-auto\">")
            sections.eachWithIndex { section, i -> 
                def value = body()
                if (value) {
                    value = StringUtils.replace(value, "[id]", section.key)
                    value = StringUtils.replace(value, "[url]", getLinkForSection(section.key))
                    value = StringUtils.replace(value, "[name]", g.message(code: section?.i18nNameKey, default: section?.name))
                    value = StringUtils.replace(value, "[description]", g.message(code: section?.descriptionKey, default: section?.description))
                }
                def css = attrs.css
                if (selectedMenuSection == section.key) {
                    css = attrs.currentcss
                }
                if (!justlinks) buf.append("<li class=\"nav-item ").append(css).append("\">")
                if (justlinks && i > 0) buf.append(" | ")
                buf.append(value)
                if (!justlinks) buf.append("</li>")
            }
            if (!justlinks) buf.append("</ul>")
            out << buf.toString()
        }
    }

    def tabs = { attrs, body ->
        def page = request.getAttribute(RequestConstants.PAGE)
        def currentSection = page.getProperty("meta.admin.active.section")
        def currentItem = page.getProperty("meta.admin.active.item")

        currentSection = StringUtils.stripToNull(currentSection)
        def selectedMenuSection = currentSection
        if(StringUtils.contains(currentSection, "/")) {
            selectedMenuSection = currentSection.substring(0, currentSection.indexOf("/"))
        }
        def bean = attrs.remove('bean')
        def justlinks = attrs.remove('justlinks')

        def sections = webInterfaceManager.getDisplayableSections(SYSTEM_ADMIN_NAVIGATION_BAR, [:])
        if (sections) {
            StringBuilder buf = new StringBuilder()
            if (!justlinks) buf.append("<ul class=\"nav nav-tabs\">")
            sections.eachWithIndex { section, i ->
                def value = body()
                if (value) {
                    value = StringUtils.replace(value, "[id]", section.key)
                    value = StringUtils.replace(value, "[url]", getLinkForSection(section.key))

                    def css = attrs.css
                    if (selectedMenuSection == section.key) {
                        css = attrs.currentcss
                    }
                    value = StringUtils.replace(value, "[class]", css)
                    value = StringUtils.replace(value, "[name]", g.message(code: section.i18nNameKey, default: section.name) ?: section.name)
                    value = StringUtils.replace(value, "[description]", g.message(code: section.descriptionKey, default: section.description) ?: section.description)
                }
                if (!justlinks) buf.append("<li class=\"nav-item\">")
                if (justlinks && i > 0) buf.append(" | ")
                buf.append(value)
                if (!justlinks) buf.append("</li>")
            }
            if (!justlinks) buf.append("</ul>")
            out << buf.toString()
        }
    }

    def sidebar = { attrs, body ->
        def page = request.getAttribute(RequestConstants.PAGE)
        def currentSection = page.getProperty("meta.admin.active.section")
        def currentItem = page.getProperty("meta.admin.active.item")

        currentSection = StringUtils.stripToNull(currentSection)
        def selectedMenuSection = currentSection
        def selectedMenuSubnav = currentSection
        if(StringUtils.contains(currentSection, "/")) {
            selectedMenuSection = currentSection.substring(0, currentSection.indexOf("/"))
            selectedMenuSubnav = currentSection.substring(currentSection.indexOf("/") + 1)
        }

        def sections = webInterfaceManager.getDisplayableSections(selectedMenuSection, [:])

        if (sections) {
            StringBuilder buf = new StringBuilder()
            sections.eachWithIndex { section, i -> 
                buf.append("<h6 class=\"text-black-50 text-uppercase\">")
                buf.append(g.message(code: section.i18nNameKey, default: section.name) ?: section.name)
                buf.append("</h6>")

                def items = webInterfaceManager.getDisplayableItems(selectedMenuSection + '/' + section.key, [:])
                if (items) {
                    buf.append("<ul class=\"nav flex-column\">")
                    items.each { item -> 
                        String value = body()
                        if (value) {
                            value = StringUtils.replace(value, "[id]", item.key)
                            value = StringUtils.replace(value, "[name]", g.message(code: item.webLabel?.key, default: item.name) ?: item.name)
                            value = StringUtils.replace(value, "[description]", item.description ?: item.name)
                            value = StringUtils.replace(value, "[url]", getLinkForItemUrl(item.link?.url))

                            def css = attrs.css
                            if (item.key == currentItem) {
                                css = attrs.currentcss
                            }
                            value = StringUtils.replace(value, "[class]", css)
                        }
                        buf.append("<li class=\"nav-item\">").append(value).append("</li>");
                    }
                    buf.append("</ul>")

                }
            }

            out << buf.toString()
        }
    }

    private String getLinkForSection(String section) {
        def sections = webInterfaceManager.getDisplayableSections(section, [:])
        def items = webInterfaceManager.getDisplayableItems(section, [:])
        if (items) {
            return getLinkForItemUrl(items[0]?.link.url)
        }
        else {
            if (sections) {
                items = webInterfaceManager.getDisplayableItems(section + '/' + sections[0].key, [:])
                if (items) {
                    return getLinkForItemUrl(items[0]?.link.url)
                }
                else {
                    return ""
                }
            }
        }
    }

    private String getLinkForItemUrl(url) {
        if (url instanceof Map) {
            return g.createLink(url as Map)
        }
        else {
            return url
        }
    }
}
