/*
 * Copyright 2004-2023 the original author or authors.
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
import groovy.util.logging.Slf4j
import org.springframework.core.Ordered
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.view.InternalResourceViewResolver

import grails.config.Config
import grails.core.gsp.GrailsTagLibClass
import grails.gsp.PageRenderer
import grails.plugins.Plugin
import grails.util.Environment
import grails.util.GrailsUtil
import grails.util.Metadata
import grails.web.pages.GroovyPagesUriService

import org.grails.core.artefact.gsp.TagLibArtefactHandler
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.gsp.jsp.TagLibraryResolverImpl
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
import org.grails.spring.RuntimeSpringConfiguration
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagLibraryMetaUtils
import org.grails.web.gsp.GroovyPagesTemplateRenderer
import org.grails.web.pages.DefaultGroovyPagesUriService
import org.grails.web.pages.FilteringCodecsByContentTypeSettings
import org.grails.web.servlet.view.GroovyPageViewResolver
import org.grails.web.sitemesh.GroovyPageLayoutFinder
import org.grails.web.util.GrailsApplicationAttributes

/**
 * Sets up and configures the GSP and GSP tag library support in Grails.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.1
 */
@Slf4j
class GroovyPagesGrailsPlugin extends Plugin implements Ordered {

    public static final String GSP_RELOAD_INTERVAL = "grails.gsp.reload.interval"
    public static final String GSP_VIEW_LAYOUT_RESOLVER_ENABLED = 'grails.gsp.view.layoutViewResolver'
    public static final String SITEMESH_DEFAULT_LAYOUT = 'grails.sitemesh.default.layout'
    public static final String SITEMESH_ENABLE_NONGSP = 'grails.sitemesh.enable.nongsp'

    int order = 600

    def watchedResources = ["file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
                            "file:./grails-app/taglib/**/*TagLib.groovy",
                            "file:./plugins/*/app/taglib/**/*TagLib.groovy",
                            "file:./app/taglib/**/*TagLib.groovy"]

    def grailsVersion = "3.3.0 > *"
    def dependsOn = [core: GrailsUtil.getGrailsVersion(), i18n: GrailsUtil.getGrailsVersion()]
    def observe = ['controllers']

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

    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    @CompileStatic
    @Override
    void doWithApplicationContext() {
        applicationContext.getBean("groovyPagesTemplateEngine", GroovyPagesTemplateEngine).clearPageCache()
    }

    /**
     * Configures the various Spring beans required by GSP
     */
    Closure doWithSpring() {
        { ->
            def application = grailsApplication
            Config config = application.config
            boolean developmentMode = isDevelopmentMode()
            Environment env = Environment.current

            boolean enableReload = env.isReloadEnabled() ||
                    config.getProperty(GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD, Boolean, false) ||
                    (developmentMode && env == Environment.DEVELOPMENT)

            long gspCacheTimeout = config.getProperty(GSP_RELOAD_INTERVAL, Long, (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L)
            boolean enableCacheResources = !config.getProperty(GroovyPagesTemplateEngine.CONFIG_PROPERTY_DISABLE_CACHING_RESOURCES, Boolean, false)
            def disableLayoutViewResolver = config.getProperty(GSP_VIEW_LAYOUT_RESOLVER_ENABLED, Boolean, true)
            String defaultDecoratorNameSetting = config.getProperty(SITEMESH_DEFAULT_LAYOUT, '')
            def sitemeshEnableNonGspViews = config.getProperty(SITEMESH_ENABLE_NONGSP, Boolean, false)

            RuntimeSpringConfiguration spring = springConfig

            // resolves JSP tag libraries
            if (ClassUtils.isPresent("org.grails.gsp.jsp.TagLibraryResolverImpl", application.classLoader)) {
                jspTagLibraryResolver(TagLibraryResolverImpl)
            }

            // resolves GSP tag libraries
            gspTagLibraryLookup(TagLibraryLookup)

            // Setup the main templateEngine used to render GSPs
            groovyPagesTemplateEngine(GroovyPagesTemplateEngine) { bean ->
                groovyPageLocator = ref('groovyPageLocator')
                if (enableReload) {
                    reloadEnabled = enableReload
                }
                tagLibraryLookup = gspTagLibraryLookup
                jspTagLibraryResolver = jspTagLibraryResolver
                cacheResources = enableCacheResources
            }

            spring.addAlias('groovyTemplateEngine', 'groovyPagesTemplateEngine')

            groovyPageRenderer(PageRenderer, ref("groovyPagesTemplateEngine")) { bean ->
                bean.lazyInit = true
                groovyPageLocator = ref('groovyPageLocator')
            }

            groovyPagesTemplateRenderer(GroovyPagesTemplateRenderer) { bean ->
                bean.autowire = true
            }

            groovyPageLayoutFinder(GroovyPageLayoutFinder) {
                gspReloadEnabled = enableReload
                defaultDecoratorName = defaultDecoratorNameSetting ?: null
                enableNonGspViews = sitemeshEnableNonGspViews
            }

            // Setup the GroovyPagesUriService
            groovyPagesUriService(DefaultGroovyPagesUriService) { bean ->
                bean.lazyInit = true
            }

            boolean jstlPresent = ClassUtils.isPresent(
                    "javax.servlet.jsp.jstl.core.Config", InternalResourceViewResolver.getClassLoader())

            abstractViewResolver {
                prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
                suffix = jstlPresent ? GroovyPageViewResolver.JSP_SUFFIX : GroovyPageViewResolver.GSP_SUFFIX
                templateEngine = groovyPagesTemplateEngine
                groovyPageLocator = ref('groovyPageLocator')
                if (enableReload) {
                    cacheTimeout = gspCacheTimeout
                }
            }
            // Configure a Spring MVC view resolver
            jspViewResolver(GroovyPageViewResolver) { bean ->
                bean.lazyInit = true
                bean.parent = "abstractViewResolver"
            }

            // "grails.gsp.view.layoutViewResolver=false" can be used to disable GrailsLayoutViewResolver
            // containsKey check must be made to check existence of boolean false values in ConfigObject

            if (disableLayoutViewResolver) {
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

    protected boolean isDevelopmentMode() {
        Metadata.getCurrent().isDevelopmentEnvironmentAvailable()
    }

    static String transformToValidLocation(String location) {
        if (location == '.') {
            return location
        }
        if (!location.endsWith(File.separator)) {
            return "${location}${File.separator}"
        }
        return location
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
        applicationContext.getBean('filteringCodecsByContentTypeSettings', FilteringCodecsByContentTypeSettings).initialize(grailsApplication)
    }

}
