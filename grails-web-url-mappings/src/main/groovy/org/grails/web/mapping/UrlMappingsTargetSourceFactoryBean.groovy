/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.web.mapping

import grails.core.GrailsApplication
import grails.plugins.GrailsPluginManager
import groovy.transform.CompileStatic
import org.grails.spring.beans.factory.HotSwappableTargetSourceFactoryBean
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext

/**
 * Creates a HotSwappableTargetSource for UrlMappings
 *
 * @author Michael Yan
 * @since 5.1
 */
@CompileStatic
class UrlMappingsTargetSourceFactoryBean extends HotSwappableTargetSourceFactoryBean {

    private GrailsApplication grailsApplication
    private GrailsPluginManager pluginManager
    private ApplicationContext applicationContext

    @Override
    void afterPropertiesSet() throws Exception {
        UrlMappingsHolderFactoryBean factoryBean = new UrlMappingsHolderFactoryBean()
        factoryBean.setApplicationContext(this.applicationContext)
        factoryBean.setGrailsApplication(this.grailsApplication)
        factoryBean.setPluginManager(this.pluginManager)
        factoryBean.afterPropertiesSet()
        setTarget(factoryBean.getObject())
        super.afterPropertiesSet()
    }

    void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager
    }

    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
        setGrailsApplication(applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class))
        setPluginManager(applicationContext.containsBean(GrailsPluginManager.BEAN_NAME) ?
                applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class) : null)
    }

}
