/*
 * Copyright 2021-2023 the original author or authors.
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
package org.grails.boot.actuate.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import grails.plugins.GrailsPluginManager;

import org.grails.boot.actuate.endpoint.PluginsEndpoint;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link PluginsEndpoint}.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(endpoint = PluginsEndpoint.class)
public class PluginsEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PluginsEndpoint pluginsEndpoint(ObjectProvider<GrailsPluginManager> pluginManagerProvider) {
        return new PluginsEndpoint(pluginManagerProvider.getIfAvailable());
    }

}
