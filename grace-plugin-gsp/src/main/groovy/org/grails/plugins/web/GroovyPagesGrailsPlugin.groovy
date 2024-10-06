/*
 * Copyright 2004-2024 the original author or authors.
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
package org.grails.plugins.web

import groovy.transform.CompileStatic
import org.springframework.core.Ordered

import grails.config.Config
import grails.core.gsp.GrailsTagLibClass
import grails.plugins.Plugin
import grails.util.GrailsUtil
import grails.web.pages.GroovyPagesUriService
import org.grails.core.artefact.gsp.TagLibArtefactHandler
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.grails.plugins.web.taglib.CountryTagLib
import org.grails.plugins.web.taglib.FormTagLib
import org.grails.plugins.web.taglib.FormatTagLib
import org.grails.plugins.web.taglib.JavascriptTagLib
import org.grails.plugins.web.taglib.PluginTagLib
import org.grails.plugins.web.taglib.RenderTagLib
import org.grails.plugins.web.taglib.SitemeshTagLib
import org.grails.plugins.web.taglib.UrlMappingTagLib
import org.grails.plugins.web.taglib.ValidationTagLib
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagLibraryMetaUtils
import org.grails.web.pages.FilteringCodecsByContentTypeSettings

/**
 * Sets up and configures the GSP and GSP tag library support in Grails.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.1
 */
class GroovyPagesGrailsPlugin extends Plugin implements Ordered {

    static final String GSP_RELOAD_INTERVAL = "grails.gsp.reload.interval"
    static final String GSP_VIEW_LAYOUT_RESOLVER_ENABLED = 'grails.gsp.view.layoutViewResolver'

    def grailsVersion = "2022.0.0 > *"
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: GrailsUtil.getGrailsVersion(), i18n: GrailsUtil.getGrailsVersion()]

    def watchedResources = ["file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
                            "file:./grails-app/taglib/**/*TagLib.groovy",
                            "file:./plugins/*/app/taglib/**/*TagLib.groovy",
                            "file:./app/taglib/**/*TagLib.groovy"]

    def providedArtefacts = [
            ApplicationTagLib,
            CountryTagLib,
            FormatTagLib,
            FormTagLib,
            JavascriptTagLib,
            RenderTagLib,
            UrlMappingTagLib,
            ValidationTagLib,
            PluginTagLib,
            SitemeshTagLib
    ]

    int order = 600

    /**
     * Configures the various Spring beans required by GSP
     */
    Closure doWithSpring() {
        { ->
            def application = grailsApplication
            Config config = application.config
            boolean enableLayoutViewResolver = config.getProperty(GSP_VIEW_LAYOUT_RESOLVER_ENABLED, Boolean, true)

            // "grails.gsp.view.layoutViewResolver=false" can be used to disable GrailsLayoutViewResolver
            // containsKey check must be made to check existence of boolean false values in ConfigObject
            if (enableLayoutViewResolver) {
                grailsLayoutViewResolverPostProcessor(GrailsLayoutViewResolverPostProcessor)
            }

            // Now go through tag libraries and configure them in Spring too. With AOP proxies and so on
            def taglibs = application.getArtefacts(TagLibArtefactHandler.TYPE)
            for (taglib in taglibs) {
                final tagLibClass = taglib.clazz

                "${taglib.fullName}"(tagLibClass) { bean ->
                    bean.autowire = true
                    bean.lazyInit = true

                    // Taglib scoping support could be easily added here. Scope could be based on a static field in the taglib class.
                    //bean.scope = 'request'
                }
            }

        }
    }

    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    @CompileStatic
    @Override
    void doWithApplicationContext() {
        applicationContext.getBean("groovyPagesTemplateEngine", GroovyPagesTemplateEngine).clearPageCache()
    }

    @Override
    void onChange(Map<String, Object> event) {
        def application = grailsApplication
        def ctx = applicationContext

        if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {
            GrailsTagLibClass taglibClass = (GrailsTagLibClass) application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                beans {
                    "$beanName"(taglibClass.clazz) { bean ->
                        bean.autowire = true
                    }
                }

                // The tag library lookup class caches "tag -> taglib class"
                // so we need to update it now.
                def lookup = applicationContext.getBean('gspTagLibraryLookup', TagLibraryLookup)
                lookup.registerTagLib(taglibClass)
                TagLibraryMetaUtils.enhanceTagLibMetaClass(taglibClass, lookup)
            }
        }
        // clear uri cache after changes
        ctx.getBean('groovyPagesUriService', GroovyPagesUriService).clear()
    }

    @CompileStatic
    void onConfigChange(Map<String, Object> event) {
        applicationContext.getBean('filteringCodecsByContentTypeSettings', FilteringCodecsByContentTypeSettings)
                .initialize(grailsApplication)
    }

}
