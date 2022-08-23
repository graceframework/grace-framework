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
package org.grails.spring.context.annotation;

import grails.plugins.GrailsPluginManager;
import grails.util.GrailsStringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.AntPathMatcher;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Extends Spring's default &lt;context:component-scan/&gt; element to ignore
 * generated classes.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 1.2
 */
public class ClosureClassIgnoringComponentScanBeanDefinitionParser extends ComponentScanBeanDefinitionParser {

    @Override
    protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        ClassPathBeanDefinitionScanner scanner = super.createScanner(readerContext, useDefaultFilters);
        BeanDefinitionRegistry beanDefinitionRegistry = readerContext.getRegistry();

        GrailsPluginManager pluginManager = null;

        if (beanDefinitionRegistry instanceof HierarchicalBeanFactory) {
            HierarchicalBeanFactory beanFactory = (HierarchicalBeanFactory) beanDefinitionRegistry;
            BeanFactory parent = beanFactory.getParentBeanFactory();
            if (parent != null && parent.containsBean(GrailsPluginManager.BEAN_NAME)) {
                pluginManager = parent.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
            }
        }

        if (pluginManager != null) {
            List<TypeFilter> typeFilters = pluginManager.getTypeFilters();
            for (TypeFilter typeFilter : typeFilters) {
                scanner.addIncludeFilter(typeFilter);
            }
        }
        return scanner;
    }

    @Override
    protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        ClassPathBeanDefinitionScanner scanner = super.configureScanner(parserContext, element);

        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        resourceResolver.setPathMatcher(new AntPathMatcher() {
            @Override
            public boolean match(String pattern, String path) {
                if (path.endsWith(".class")) {
                    String filename = GrailsStringUtils.getFileBasename(path);
                    if (filename.contains("$")) {
                        return false;
                    }
                }
                return super.match(pattern, path);
            }
        });
        scanner.setResourceLoader(resourceResolver);
        return scanner;
    }
}
