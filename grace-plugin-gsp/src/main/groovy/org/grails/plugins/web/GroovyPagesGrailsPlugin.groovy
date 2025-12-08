/*
 * Copyright 2004-2025 the original author or authors.
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

import grails.plugins.Plugin
import grails.util.GrailsUtil
import grails.web.pages.GroovyPagesUriService
import org.grails.gsp.GroovyPagesTemplateEngine
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
    def dependsOn = [core: GrailsUtil.getGrailsVersion()]

    int order = 600

    /**
     * Configures the various Spring beans required by GSP
     */
    Closure doWithSpring() { { ->

    } }

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
        def ctx = applicationContext

        // clear uri cache after changes
        ctx.getBean('groovyPagesUriService', GroovyPagesUriService).clear()
    }

    @CompileStatic
    void onConfigChange(Map<String, Object> event) {
        applicationContext.getBean('filteringCodecsByContentTypeSettings', FilteringCodecsByContentTypeSettings)
                .initialize(grailsApplication)
    }

}
