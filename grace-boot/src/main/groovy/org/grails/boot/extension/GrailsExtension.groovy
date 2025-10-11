/*
 * Copyright 2021-2025 the original author or authors.
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
package org.grails.boot.extension

import groovy.transform.CompileStatic

import grails.artefact.ArtefactTypes
import grails.boot.Grails
import grails.plugins.GrailsPlugin
import grails.util.Environment
import grails.util.Holders
import grails.util.Metadata

/**
 * An extension that adds methods to the {@link Grails} object.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@CompileStatic
class GrailsExtension {

    static String getInfo(Grails grails) {
        new StringBuilder()
                .append('\nApplication: ').append(Metadata.current.getApplicationName())
                .append('\nVersion:     ').append(Metadata.current.getApplicationVersion())
                .append('\nEnvironment: ').append(Environment.current.name)
                .append('\n')
                .toString()
    }

    static List<String> getControllers(Grails grails) {
        Holders.getGrailsApplication().getArtefacts(ArtefactTypes.CONTROLLER)*.name?.toSorted()
    }

    static List<String> getDomains(Grails grails) {
        Holders.getGrailsApplication().getArtefacts(ArtefactTypes.DOMAIN_CLASS)*.name?.toSorted()
    }

    static List<String> getServices(Grails grails) {
        Holders.getGrailsApplication().getArtefacts(ArtefactTypes.SERVICE)*.name?.toSorted()
    }

    static List<String> getTaglibs(Grails grails) {
        Holders.getGrailsApplication().getArtefacts(ArtefactTypes.TAG_LIBRARY)*.name?.toSorted()
    }

    static List<String> getUrlMappings(Grails grails) {
        Holders.getGrailsApplication().getArtefacts(ArtefactTypes.URL_MAPPINGS)*.name?.toSorted()
    }

    static List<GrailsPlugin> getPlugins(Grails grails) {
        Holders.getPluginManager().getAllPlugins().toList()
    }

}
