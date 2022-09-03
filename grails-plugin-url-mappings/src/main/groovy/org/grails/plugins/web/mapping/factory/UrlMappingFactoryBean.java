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
package org.grails.plugins.web.mapping.factory;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A factory bean that creates the URL mappings, checking if there is a bean
 * called urlMap in the ctx and merging that with the mappings set explicitly
 * on this bean.
 *
 * @author Graeme Rocher
 * @since 0.3
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UrlMappingFactoryBean extends AbstractFactoryBean<Map> implements ApplicationContextAware {

    private static final Log log = LogFactory.getLog(UrlMappingFactoryBean.class);

    private static final String URL_MAP_BEAN = "urlMappings";

    private ApplicationContext applicationContext;

    private Map mappings = new HashMap();

    @Override
    protected Map createInstance() {
        if (this.applicationContext.containsBean(UrlMappingFactoryBean.URL_MAP_BEAN)) {
            Object o = this.applicationContext.getBean(UrlMappingFactoryBean.URL_MAP_BEAN);
            if (o instanceof Map) {
                this.mappings.putAll((Map) o);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("[UrlMappingFactoryBean] Creating URL mappings as...");
            for (Object key : this.mappings.keySet()) {
                log.debug("[UrlMappingFactoryBean] " + key + "=" + this.mappings.get(key));
            }
        }
        return this.mappings;
    }

    @Override
    public Class<?> getObjectType() {
        return Map.class;
    }

    public void setMappings(Map mappings) {
        this.mappings = mappings;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
