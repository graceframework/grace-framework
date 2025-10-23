/*
 * Copyright 2024 the original author or authors.
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
package grails.plugin.cache;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import grails.config.Config;
import grails.core.GrailsApplication;
import org.grails.plugin.cache.GrailsCacheManager;

/**
 * {@link ApplicationListener} to cleanup caches once the context is loaded.
 *
 * @author Michael Yan
 * @since 6.1
 */
public class ClearCachesApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ClearCachesApplicationListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        GrailsCacheManager grailsCacheManager = applicationContext.getBean("grailsCacheManager", GrailsCacheManager.class);
        GrailsApplication grailsApplication = applicationContext.getBean(GrailsApplication.class);
        Config config = grailsApplication.getConfig();

        boolean enabled = config.getProperty("grails.cache.enabled", Boolean.class, Boolean.TRUE);
        if (enabled) {
            boolean clearAtStartup = config.getProperty("grails.cache.clearAtStartup", Boolean.class, Boolean.FALSE);
            if (clearAtStartup) {
                for (String cacheName : grailsCacheManager.getCacheNames()) {
                    Cache cache = grailsCacheManager.getCache(cacheName);
                    if (cache != null) {
                        log.info("Clearing cache {}", cacheName);
                        cache.clear();
                    }
                }
            }

            List<String> defaultCaches = List.of("grailsBlocksCache", "grailsTemplatesCache");
            for (String name : defaultCaches) {
                if (!grailsCacheManager.cacheExists(name)) {
                    grailsCacheManager.getCache(name);
                }
            }
        }
    }

}
