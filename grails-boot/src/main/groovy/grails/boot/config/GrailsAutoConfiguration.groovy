/*
 * Copyright 2015-2022 the original author or authors.
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
package grails.boot.config

import java.lang.reflect.Field

import groovy.transform.CompileStatic
import org.springframework.aop.config.AopConfigUtils

import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.grails.spring.aop.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator

/**
 * A base class for configurations that bootstrap a Grails application
 *
 * @since 3.0
 * @author Graeme Rocher
 *
 */
@CompileStatic
class GrailsAutoConfiguration {

    private static final String APC_PRIORITY_LIST_FIELD = 'APC_PRIORITY_LIST'

    static {
        try {
            // patch AopConfigUtils if possible
            Field field = AopConfigUtils.getDeclaredField(APC_PRIORITY_LIST_FIELD)
            if (field != null) {
                field.setAccessible(true)
                Object obj = field.get(null)
                List<Class<?>> list = (List<Class<?>>) obj
                list.add(GroovyAwareInfrastructureAdvisorAutoProxyCreator)
                list.add(GroovyAwareAspectJAwareAdvisorAutoProxyCreator)
            }
        }
        catch (Throwable ignored) {
        }
    }

}
