/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.boot.context.web;

import java.util.Map;

import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;

import grails.artefact.ArtefactTypes;
import grails.core.GrailsApplication;
import grails.core.GrailsApplicationLifeCycleAdapter;
import grails.core.GrailsClass;
import grails.core.support.GrailsApplicationAware;
import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;
import grails.web.servlet.bootstrap.GrailsBootstrapClass;

import org.grails.web.servlet.context.GrailsConfigUtils;

/**
 * Runs the Bootstrap classes on startup
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
public class GrailsBootstrapClassRunner extends GrailsApplicationLifeCycleAdapter
        implements GrailsApplicationAware, ServletContextAware, ApplicationContextAware, PluginManagerAware {

    private static final Logger log = LoggerFactory.getLogger(GrailsBootstrapClassRunner.class);

    private GrailsApplication grailsApplication;
    private GrailsPluginManager pluginManager;
    private ApplicationContext applicationContext;
    private ServletContext servletContext;

    @Override
    public void onStartup(Map<String, Object> event) {
        if (this.grailsApplication != null && this.applicationContext != null) {
            GrailsConfigUtils.executeGrailsBootstraps(this.grailsApplication, (WebApplicationContext) this.applicationContext, this.servletContext, this.pluginManager);
        }
    }

    @Override
    public void onShutdown(Map<String, Object> event) {
        if (this.grailsApplication != null && this.applicationContext != null) {
            for (GrailsClass cls : this.grailsApplication.getArtefacts(ArtefactTypes.BOOTSTRAP)) {
                try {
                    ((GrailsBootstrapClass) cls).callDestroy();
                }
                catch (Throwable e) {
                    log.error("Error occurred running Bootstrap destroy method: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}
