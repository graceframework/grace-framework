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

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.context.annotation.Bean;

import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator;
import org.grails.spring.aop.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator;
import org.grails.spring.aop.autoproxy.GroovyAwareAutoProxyCreatorPostProcessor;

/**
 * {@link EnableAutoConfiguration Auto-configure} for Groovy-aware AutoProxy
 *
 * @author Michael Yan
 * @since 2023.1
 */
@AutoConfiguration(after = AopAutoConfiguration.class)
public class GroovyAopAutoConfiguration {

    private static final String APC_PRIORITY_LIST_FIELD = "APC_PRIORITY_LIST";

    static {
        try {
            // patch AopConfigUtils if possible
            Field field = AopConfigUtils.class.getDeclaredField(APC_PRIORITY_LIST_FIELD);
            if (field != null) {
                field.setAccessible(true);
                Object obj = field.get(null);
                List<Class<?>> list = (List<Class<?>>) obj;
                list.add(GroovyAwareInfrastructureAdvisorAutoProxyCreator.class);
                list.add(GroovyAwareAspectJAwareAdvisorAutoProxyCreator.class);
            }
        } catch (Throwable ignore) {
        }
    }

    @Bean
    public static GroovyAwareAutoProxyCreatorPostProcessor groovyAwareAutoProxyCreatorPostProcessor() {
        return new GroovyAwareAutoProxyCreatorPostProcessor();
    }

}
