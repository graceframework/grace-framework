package org.grails.plugins.core

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.grails.config.PropertySourcesConfig
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.support.MockApplicationContext
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockServletContext
import org.springframework.util.ClassUtils

class CoreGrailsPluginTests {
    GroovyClassLoader gcl = new GroovyClassLoader()
    DefaultGrailsApplication ga
    MockApplicationContext ctx

    @BeforeEach
    protected void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally()

        ctx = new MockApplicationContext()
        ctx.registerMockBean(GrailsApplication.CLASS_LOADER_BEAN, gcl)
        onSetUp()
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl)
        if(ClassUtils.isPresent("Config", gcl)) {
            ConfigObject config = new ConfigSlurper().parse(gcl.loadClass("Config"))
            ga.setConfig(new PropertySourcesConfig(config))
        }
        ga.setApplicationContext(ctx)
        ga.initialise()
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
    }

    @AfterEach
    protected void tearDown() throws Exception {
        ExpandoMetaClass.disableGlobally();
    }

    @Test
    void testCorePlugin() {
        def pluginClass = gcl.loadClass("org.grails.plugins.core.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("classLoader")
        assert appCtx.containsBean("customEditors")
    }

    @Test
    void testDisableAspectj() {
        def pluginClass = gcl.loadClass("org.grails.plugins.core.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()
        ga.config['grails.spring.disable.aspectj.autoweaving'] = true
        ga.configChanged()
        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("classLoader")
        assert appCtx.containsBean("customEditors")
    }

    protected void onSetUp() {
        // needed for testBeanPropertyOverride
        gcl.parseClass("""
            class SomeTransactionalService {
                boolean transactional = true
                Integer i
            }
            class NonTransactionalService {
                boolean transactional = false
                Integer i
            }
        """)
    }

    protected static MockServletContext createMockServletContext() {
        return new MockServletContext()
    }

    protected static MockApplicationContext createMockApplicationContext() {
        return new MockApplicationContext()
    }

    protected static Resource[] getResources(String pattern) throws IOException {
        return new PathMatchingResourcePatternResolver().getResources(pattern)
    }

    protected static MessageSource createMessageSource() {
        return new StaticMessageSource()
    }
}
