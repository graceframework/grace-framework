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
package grails.plugin.scaffolding;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.util.Environment;
import org.grails.gsp.GroovyPagesTemplateEngine;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;
import org.grails.web.servlet.view.GroovyPageViewResolver;
import org.grails.web.util.GrailsApplicationAttributes;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for Scaffolding
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@AutoConfigureOrder(-10)
public class ScaffoldingAutoConfiguration {

    private static final String GSP_RELOAD_INTERVAL = "grails.gsp.reload.interval";

    @Bean(name = { "jspViewResolver", "scaffoldingViewResolver" })
    @ConditionalOnMissingBean
    public GroovyPageViewResolver scaffoldingViewResolver(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<GrailsConventionGroovyPageLocator> groovyPageLocator,
            ObjectProvider<GroovyPagesTemplateEngine> groovyPagesTemplateEngine) {
        Config config = grailsApplication.getObject().getConfig();
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        long gspCacheTimeout = config.getProperty(GSP_RELOAD_INTERVAL, Long.class,
                (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L);

        boolean jstlPresent = ClassUtils.isPresent("jakarta.servlet.jsp.jstl.core.Config", getClass().getClassLoader());

        GroovyPageViewResolver groovyPageViewResolver = new ScaffoldingViewResolver();
        groovyPageViewResolver.setGroovyPageLocator(groovyPageLocator.getObject());
        groovyPageViewResolver.setTemplateEngine(groovyPagesTemplateEngine.getObject());
        groovyPageViewResolver.setPrefix(GrailsApplicationAttributes.PATH_TO_VIEWS);
        groovyPageViewResolver.setSuffix(jstlPresent ? GroovyPageViewResolver.JSP_SUFFIX : GroovyPageViewResolver.GSP_SUFFIX);

        if (enableReload) {
            groovyPageViewResolver.setCacheTimeout(gspCacheTimeout);
        }

        return groovyPageViewResolver;
    }
}
