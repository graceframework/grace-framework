/*
 * Copyright 2015-2023 the original author or authors.
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
package grails.boot.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Grails.
 *
 * @author Michael Yan
 * @since 2022.0.0
 * @see org.springframework.context.annotation.Configuration
 * @see GrailsApplicationPostProcessor
 */
@AutoConfiguration
@AutoConfigureOrder(10000)
@Import(GrailsAutoConfiguration.GrailsRegistrar.class)
public class GrailsAutoConfiguration {

    static class GrailsRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(@Nullable AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            registerGrailsApplicationPostProcessor(registry);
        }

        private void registerGrailsApplicationPostProcessor(BeanDefinitionRegistry registry) {
            if (!registry.containsBeanDefinition(GrailsApplicationPostProcessor.BEAN_NAME)) {
                BeanDefinitionBuilder postProcessorBuilder =
                        BeanDefinitionBuilder.genericBeanDefinition(GrailsApplicationPostProcessor.class, GrailsApplicationPostProcessor::new)
                                .setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

                registry.registerBeanDefinition(GrailsApplicationPostProcessor.BEAN_NAME, postProcessorBuilder.getBeanDefinition());
            }
        }

    }

}
