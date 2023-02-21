/*
 * Copyright 2013-2023 the original author or authors.
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
package org.grails.plugins.web.rest.plugin

import groovy.transform.CompileStatic
import org.springframework.core.PriorityOrdered

import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.plugins.Plugin
import grails.rest.Resource
import grails.util.GrailsUtil

import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.artefact.DomainClassArtefactHandler

/**
 * @since 2.3
 * @author Graeme Rocher
 */
class RestResponderGrailsPlugin extends Plugin implements PriorityOrdered {

    def version = GrailsUtil.getGrailsVersion()
    def loadBefore = ['controllers']
    def observe = ['domainClass']

    GrailsApplication grailsApplication

    @Override
    Closure doWithSpring() {
        { ->
            def application = grailsApplication
            registryResourceControllers(application)
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        if (!(event.source instanceof Class)) {
            return
        }
        registryResourceControllers(grailsApplication)
    }

    @CompileStatic
    static void registryResourceControllers(GrailsApplication app) {
        for (GrailsClass grailsClass in app.getArtefacts(DomainClassArtefactHandler.TYPE)) {
            Class<?> clazz = grailsClass.clazz
            if (clazz.getAnnotation(Resource)) {
                String controllerClassName = "${clazz.name}Controller"
                if (!app.getArtefact(ControllerArtefactHandler.TYPE, controllerClassName)) {
                    try {
                        app.addArtefact(ControllerArtefactHandler.TYPE, app.classLoader.loadClass(controllerClassName))
                    }
                    catch (ClassNotFoundException ignored) {
                    }
                }
            }
        }
    }

    @Override
    int getOrder() {
        40
    }

}
