/*
 * Copyright 2021-2022 the original author or authors.
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
package org.grails.plugins.support;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;

/**
 * Print information of Grails plugins when application has been started.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsPluginsInfoApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ApplicationStartedEvent> {

    private static final Log logger = LogFactory.getLog(GrailsPluginsInfoApplicationContextInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.addApplicationListener(this);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();

        if (!applicationContext.containsBean(GrailsPluginManager.BEAN_NAME)) {
            return;
        }

        GrailsPluginManager pluginManager = applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);

        List<GrailsPlugin> allPlugins = pluginManager.getPluginList();

        if (allPlugins.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("%n----------------------------------------------------------------------------------------------%n");
        sb.append("Order      Plugin Name                              Plugin Version                     Enabled");
        sb.append("%n----------------------------------------------------------------------------------------------");
        for (int i = 0; i < allPlugins.size(); i++) {
            GrailsPlugin plugin = allPlugins.get(i);
            boolean enabled = plugin.isEnabled() && (pluginManager.getFailedPlugin(plugin.getName()) == null);
            sb.append(String.format("%n%s      %s%s%s",
                    StringUtils.leftPad(String.valueOf(i + 1), 5),
                    StringUtils.rightPad(StringUtils.capitalize(plugin.getName()), 41),
                    StringUtils.rightPad(plugin.getVersion(), 41),
                    enabled ? "Y" : "N"));
        }
        sb.append("%n----------------------------------------------------------------------------------------------%n");

        if (logger.isDebugEnabled()) {
            logger.debug(String.format(sb.toString()));
        }
    }

}
