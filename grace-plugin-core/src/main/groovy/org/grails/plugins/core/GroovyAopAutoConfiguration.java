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
package org.grails.plugins.core;

import org.aopalliance.aop.Advice;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import grails.config.Config;
import grails.core.GrailsApplication;
import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator;
import org.grails.spring.aop.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator;

/**
 * {@link EnableAutoConfiguration Auto-configure} for Groovy-aware AutoProxy
 *
 * @author Michael Yan
 * @since 2023.1
 */
@AutoConfiguration(before = AopAutoConfiguration.class)
@AutoConfigureOrder
public class GroovyAopAutoConfiguration {

    private static final String SPRING_PROXY_TARGET_CLASS_CONFIG = "spring.aop.proxy-target-class";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Advice.class)
    static class AspectJAutoProxyingConfiguration {

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(org.aspectj.lang.annotation.Around.class)
        @ConditionalOnProperty(name = "grails.spring.disable.aspectj.autoweaving", havingValue = "false", matchIfMissing = true)
        static class AspectJAutoProxyConfiguration {

            @Bean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
            @ConditionalOnMissingBean(name = AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
            public GroovyAwareAspectJAwareAdvisorAutoProxyCreator groovyAwareAutoProxyCreator(ObjectProvider<GrailsApplication> grailsApplication) {
                Config config = grailsApplication.getObject().getConfig();
                Boolean isProxyTargetClass = config.getProperty(SPRING_PROXY_TARGET_CLASS_CONFIG, Boolean.class, false);
                GroovyAwareAspectJAwareAdvisorAutoProxyCreator autoProxyCreator = new GroovyAwareAspectJAwareAdvisorAutoProxyCreator();
                autoProxyCreator.setProxyTargetClass(isProxyTargetClass);
                return autoProxyCreator;
            }

        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(name = "grails.spring.disable.aspectj.autoweaving", havingValue = "true")
        static class InfrastructureAdvisorAutoProxyConfiguration {

            @Bean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
            @ConditionalOnMissingBean(name = AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
            public GroovyAwareInfrastructureAdvisorAutoProxyCreator groovyAwareAutoProxyCreator(ObjectProvider<GrailsApplication> grailsApplication) {
                Config config = grailsApplication.getObject().getConfig();
                Boolean isProxyTargetClass = config.getProperty(SPRING_PROXY_TARGET_CLASS_CONFIG, Boolean.class, false);
                GroovyAwareInfrastructureAdvisorAutoProxyCreator autoProxyCreator = new GroovyAwareInfrastructureAdvisorAutoProxyCreator();
                autoProxyCreator.setProxyTargetClass(isProxyTargetClass);
                return autoProxyCreator;
            }

        }

    }

}
