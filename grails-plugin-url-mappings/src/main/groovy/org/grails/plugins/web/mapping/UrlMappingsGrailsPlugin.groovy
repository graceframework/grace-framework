/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.plugins.web.mapping

import groovy.transform.CompileDynamic
import org.springframework.context.ApplicationContext

import grails.plugins.Plugin
import grails.util.GrailsUtil
import grails.web.mapping.LinkGenerator

import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.grails.web.mapping.CachingLinkGenerator

/**
 * Handles the configuration of URL mappings.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.4
 */
class UrlMappingsGrailsPlugin extends Plugin {

    def watchedResources = ['file:./grails-app/controllers/*UrlMappings.groovy',
                            'file:./app/controllers/*UrlMappings.groovy']

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version]
    def loadAfter = ['controllers']

    Closure doWithSpring() {
        { ->
            def ctx = applicationContext
            def application = grailsApplication
            if (!application.getArtefacts(UrlMappingsArtefactHandler.TYPE)) {
                application.addArtefact(UrlMappingsArtefactHandler.TYPE, DefaultUrlMappings)
            }
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        def application = grailsApplication
        if (!application.isArtefactOfType(UrlMappingsArtefactHandler.TYPE, event.source)) {
            return
        }

        application.addArtefact(UrlMappingsArtefactHandler.TYPE, event.source)

        ApplicationContext ctx = applicationContext

        LinkGenerator linkGenerator = ctx.getBean('grailsLinkGenerator', LinkGenerator)
        if (linkGenerator instanceof CachingLinkGenerator) {
            linkGenerator.clearCache()
        }
    }

    @CompileDynamic
    static class DefaultUrlMappings {

        static mappings = {
            "/$controller/$action?/$id?(.$format)?"()
        }

    }

}
