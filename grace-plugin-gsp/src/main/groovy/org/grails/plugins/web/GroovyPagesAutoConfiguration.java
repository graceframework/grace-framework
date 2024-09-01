/*
 * Copyright 2022-2024 the original author or authors.
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
package org.grails.plugins.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.gsp.PageRenderer;
import grails.util.BuildSettings;
import grails.util.Environment;

import org.grails.core.io.ResourceLocator;
import org.grails.gsp.GroovyPageResourceLoader;
import org.grails.gsp.GroovyPagesTemplateEngine;
import org.grails.gsp.io.CachingGroovyPageStaticResourceLocator;
import org.grails.gsp.io.GroovyPageLocator;
import org.grails.gsp.jsp.TagLibraryResolver;
import org.grails.gsp.jsp.TagLibraryResolverImpl;
import org.grails.taglib.TagLibraryLookup;
import org.grails.web.errors.ErrorsViewStackTracePrinter;
import org.grails.web.gsp.GroovyPagesTemplateRenderer;
import org.grails.web.gsp.io.CachingGrailsConventionGroovyPageLocator;
import org.grails.web.pages.DefaultGroovyPagesUriService;
import org.grails.web.pages.FilteringCodecsByContentTypeSettings;
import org.grails.web.pages.GroovyPagesServlet;
import org.grails.web.pages.StandaloneTagLibraryLookup;
import org.grails.web.servlet.view.GroovyPageViewResolver;
import org.grails.web.sitemesh.GroovyPageLayoutFinder;
import org.grails.web.util.GrailsApplicationAttributes;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Groovy Pages
 *
 * @author Michael Yan
 * @since 2022.2.3
 */
@AutoConfiguration
@AutoConfigureOrder
public class GroovyPagesAutoConfiguration {

    private static final String GSP_RELOAD_INTERVAL = "grails.gsp.reload.interval";
    private static final String GSP_VIEWS_DIR = "grails.gsp.view.dir";
    private static final String SITEMESH_DEFAULT_LAYOUT = "grails.sitemesh.default.layout";
    private static final String SITEMESH_ENABLE_NONGSP = "grails.sitemesh.enable.nongsp";

    @Bean
    @ConditionalOnMissingBean
    public GroovyPageResourceLoader groovyPageResourceLoader(ObjectProvider<GrailsApplication> grailsApplication) throws Exception {
        Config config = grailsApplication.getIfAvailable().getConfig();
        String viewsDir = config.getProperty(GSP_VIEWS_DIR, "");
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        boolean warDeployed = Environment.isWarDeployed();
        boolean warDeployedWithReload = warDeployed && enableReload;

        GroovyPageResourceLoader groovyPageResourceLoader = new GroovyPageResourceLoader();
        Resource baseResource;
        if (StringUtils.hasText(viewsDir)) {
            baseResource = new FileUrlResource(ResourceUtils.getURL(viewsDir));
        }
        else if (warDeployedWithReload && env.hasReloadLocation()) {
            baseResource = new FileUrlResource(ResourceUtils.getURL(env.getReloadLocation()));
        }
        else {
            baseResource = new FileUrlResource(ResourceUtils.getURL(BuildSettings.BASE_DIR.getCanonicalPath()));
        }
        groovyPageResourceLoader.setBaseResource(baseResource);

        return groovyPageResourceLoader;
    }

    @Bean
    @ConditionalOnMissingBean
    public CachingGrailsConventionGroovyPageLocator groovyPageLocator(ObjectProvider<GrailsApplication> grailsApplication,
            GroovyPageResourceLoader groovyPageResourceLoader) {

        Config config = grailsApplication.getIfAvailable().getConfig();
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        long gspCacheTimeout = config.getProperty(GSP_RELOAD_INTERVAL, Long.class, (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L);

        ResourceLoader resourceLoader = new DefaultResourceLoader();

        CachingGrailsConventionGroovyPageLocator groovyPageLocator = new CachingGrailsConventionGroovyPageLocator();
        groovyPageLocator.setResourceLoader(groovyPageResourceLoader);

        if (!developmentMode) {
            Resource defaultViews = resourceLoader.getResource("gsp/views.properties");
            if (!defaultViews.exists()) {
                defaultViews = resourceLoader.getResource("classpath:gsp/views.properties");
            }
            if (defaultViews.exists()) {
                try {
                    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
                    pfb.setIgnoreResourceNotFound(true);
                    pfb.setLocation(defaultViews);
                    pfb.afterPropertiesSet();
                    Map<String, String> precompiledGspMap = new HashMap<>();
                    CollectionUtils.mergePropertiesIntoMap(pfb.getObject(), precompiledGspMap);
                    groovyPageLocator.setPrecompiledGspMap(precompiledGspMap);
                }
                catch (IOException ignored) {
                }
            }
        }
        if (enableReload) {
            groovyPageLocator.setCacheTimeout(gspCacheTimeout);
        }
        groovyPageLocator.setReloadEnabled(enableReload);

        return groovyPageLocator;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ResourceLocator grailsResourceLocator(ObjectProvider<GrailsApplication> grailsApplication) {
        Config config = grailsApplication.getIfAvailable().getConfig();
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        long gspCacheTimeout = config.getProperty(GSP_RELOAD_INTERVAL, Long.class, (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L);

        CachingGroovyPageStaticResourceLocator groovyPageStaticResourceLocator = new CachingGroovyPageStaticResourceLocator();

        if (enableReload) {
            groovyPageStaticResourceLocator.setCacheTimeout(gspCacheTimeout);
        }

        return groovyPageStaticResourceLocator;
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorsViewStackTracePrinter errorsViewStackTracePrinter(@Qualifier("grailsResourceLocator") ResourceLocator grailsResourceLocator) {
        return new ErrorsViewStackTracePrinter(grailsResourceLocator);
    }

    @Bean
    @ConditionalOnMissingBean
    public FilteringCodecsByContentTypeSettings filteringCodecsByContentTypeSettings(ObjectProvider<GrailsApplication> grailsApplication) {

        return new FilteringCodecsByContentTypeSettings(grailsApplication.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultGrailsTagDateHelper grailsTagDateHelper() {
        return new DefaultGrailsTagDateHelper();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ServletRegistrationBean<GroovyPagesServlet> groovyPagesServlet() {
        ServletRegistrationBean<GroovyPagesServlet> servletRegistration = new ServletRegistrationBean<>();
        servletRegistration.setServlet(new GroovyPagesServlet());
        servletRegistration.setUrlMappings(List.of("*.gsp"));
        if (Environment.isDevelopmentMode()) {
            servletRegistration.addInitParameter("showSource", "1");
        }
        return servletRegistration;
    }

    @Bean
    @ConditionalOnClass(name = "org.grails.gsp.jsp.TagLibraryResolverImpl")
    public TagLibraryResolver jspTagLibraryResolver(ObjectProvider<GrailsApplication> grailsApplication) {
        TagLibraryResolverImpl tagLibraryResolver = new TagLibraryResolverImpl();
        grailsApplication.ifAvailable(tagLibraryResolver::setGrailsApplication);
        return tagLibraryResolver;
    }

    @Bean
    @Order(0)
    @ConditionalOnMissingBean
    public GroovyPageViewResolver jspViewResolver(ObjectProvider<GrailsApplication> grailsApplication,
            CachingGrailsConventionGroovyPageLocator groovyPageLocator,
            GroovyPagesTemplateEngine groovyPagesTemplateEngine) {

        Config config = grailsApplication.getIfAvailable().getConfig();
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        long gspCacheTimeout = config.getProperty(GSP_RELOAD_INTERVAL, Long.class,
                (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L);

        boolean jstlPresent = ClassUtils.isPresent("javax.servlet.jsp.jstl.core.Config", getClass().getClassLoader());

        GroovyPageViewResolver groovyPageViewResolver = new GroovyPageViewResolver();
        groovyPageViewResolver.setGroovyPageLocator(groovyPageLocator);
        groovyPageViewResolver.setTemplateEngine(groovyPagesTemplateEngine);
        groovyPageViewResolver.setPrefix(GrailsApplicationAttributes.PATH_TO_VIEWS);
        groovyPageViewResolver.setSuffix(jstlPresent ? GroovyPageViewResolver.JSP_SUFFIX : GroovyPageViewResolver.GSP_SUFFIX);

        if (enableReload) {
            groovyPageViewResolver.setCacheTimeout(gspCacheTimeout);
        }

        return groovyPageViewResolver;
    }

    @Bean
    @ConditionalOnMissingBean
    public StandaloneTagLibraryLookup gspTagLibraryLookup(ObjectProvider<GrailsApplication> grailsApplication) {
        StandaloneTagLibraryLookup tagLibraryLookup = new StandaloneTagLibraryLookup();
        grailsApplication.ifAvailable(tagLibraryLookup::setGrailsApplication);
        return tagLibraryLookup;
    }

    @Bean({"groovyTemplateEngine", "groovyPagesTemplateEngine"})
    @ConditionalOnMissingBean
    public GroovyPagesTemplateEngine groovyPagesTemplateEngine(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<TagLibraryLookup> gspTagLibraryLookup,
            ObjectProvider<TagLibraryResolver> jspTagLibraryResolver,
            ObjectProvider<GroovyPageLocator> groovyPageLocator) {
        Config config = grailsApplication.getIfAvailable().getConfig();
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        boolean enableCacheResources = !config.getProperty(Settings.GSP_DISABLE_CACHING_RESOURCES, Boolean.class, false);

        GroovyPagesTemplateEngine groovyPagesTemplateEngine = new GroovyPagesTemplateEngine();

        groovyPagesTemplateEngine.setReloadEnabled(enableReload);
        groovyPagesTemplateEngine.setCacheResources(enableCacheResources);
        groovyPageLocator.ifAvailable(groovyPagesTemplateEngine::setGroovyPageLocator);
        gspTagLibraryLookup.ifAvailable(groovyPagesTemplateEngine::setTagLibraryLookup);
        jspTagLibraryResolver.ifAvailable(groovyPagesTemplateEngine::setJspTagLibraryResolver);

        return groovyPagesTemplateEngine;
    }

    @Bean
    @ConditionalOnMissingBean
    public GroovyPagesTemplateRenderer groovyPagesTemplateRenderer(CachingGrailsConventionGroovyPageLocator groovyPageLocator,
            GroovyPagesTemplateEngine groovyPagesTemplateEngine) {
        GroovyPagesTemplateRenderer groovyPagesTemplateRenderer = new GroovyPagesTemplateRenderer();
        groovyPagesTemplateRenderer.setGroovyPageLocator(groovyPageLocator);
        groovyPagesTemplateRenderer.setGroovyPagesTemplateEngine(groovyPagesTemplateEngine);
        return groovyPagesTemplateRenderer;
    }

    @Bean
    @ConditionalOnMissingBean
    public PageRenderer groovyPageRenderer(CachingGrailsConventionGroovyPageLocator groovyPageLocator,
            GroovyPagesTemplateEngine groovyPagesTemplateEngine) {
        PageRenderer pageRenderer = new PageRenderer(groovyPagesTemplateEngine);
        pageRenderer.setGroovyPageLocator(groovyPageLocator);
        return pageRenderer;
    }

    @Bean
    @ConditionalOnMissingBean
    public GroovyPageLayoutFinder groovyPageLayoutFinder(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<GroovyPageViewResolver> jspViewResolver) {
        Config config = grailsApplication.getIfAvailable().getConfig();
        Environment env = Environment.getCurrent();
        boolean developmentMode = Environment.isDevelopmentEnvironmentAvailable();
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean.class, false);
        boolean enableReload = env.isReloadEnabled() || gspEnableReload || (developmentMode && env == Environment.DEVELOPMENT);
        String defaultDecoratorName = config.getProperty(SITEMESH_DEFAULT_LAYOUT, "application");
        Boolean sitemeshEnableNonGspViews = config.getProperty(SITEMESH_ENABLE_NONGSP, Boolean.class, false);

        GroovyPageLayoutFinder groovyPageLayoutFinder = new GroovyPageLayoutFinder();
        groovyPageLayoutFinder.setGspReloadEnabled(enableReload);
        groovyPageLayoutFinder.setDefaultDecoratorName(defaultDecoratorName);
        groovyPageLayoutFinder.setEnableNonGspViews(sitemeshEnableNonGspViews);
        jspViewResolver.ifAvailable(groovyPageLayoutFinder::setViewResolver);

        return groovyPageLayoutFinder;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultGroovyPagesUriService groovyPagesUriService() {
        return new DefaultGroovyPagesUriService();
    }

}
