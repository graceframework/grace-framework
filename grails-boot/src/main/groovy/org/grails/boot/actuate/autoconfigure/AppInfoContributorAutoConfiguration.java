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
package org.grails.boot.actuate.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import org.grails.boot.actuate.AppInfoContributor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for standard
 * {@link InfoContributor}s.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(InfoContributorAutoConfiguration.class)
public class AppInfoContributorAutoConfiguration {

    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Bean
    @ConditionalOnEnabledInfoContributor("app")
    @ConditionalOnMissingBean
    @Order(DEFAULT_ORDER)
    public AppInfoContributor appInfoContributor() {
        return new AppInfoContributor();
    }

}
