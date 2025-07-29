package org.grails.plugins.metadata

import grails.core.DefaultGrailsApplication
import grails.plugins.DefaultGrailsPluginManager
import grails.plugins.metadata.GrailsPlugin
import org.grails.plugins.TestGrailsPlugin
import org.grails.plugins.TestTwoGrailsPlugin
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class GrailsPluginMetadataTests {

    @Test
    void testAnnotatedMetadata() {
        def app = new DefaultGrailsApplication([Test1, Test2, Test3] as Class[], getClass().classLoader)
        def pluginManager = new DefaultGrailsPluginManager([TestGrailsPlugin, TestTwoGrailsPlugin] as Class[], app)
        pluginManager.loadCorePlugins = false
        pluginManager.loadPlugins()

        assertEquals "/plugins/test-1.0", pluginManager.getPluginPathForClass(Test1)
        assertNull pluginManager.getPluginPathForClass(Test3)

        assertEquals "/plugins/test-two-1.2", pluginManager.getPluginPathForInstance(new Test2())
        assertNull pluginManager.getPluginPathForInstance(new Test3())

        assertEquals "/plugins/test-1.0/grails-app/views", pluginManager.getPluginViewsPathForClass(Test1)
        assertNull pluginManager.getPluginViewsPathForClass(Test3)
    }
}

@GrailsPlugin(name='test', version='1.0')
class Test1 {}
@GrailsPlugin(name='testTwo', version='1.2')
class Test2 {}
class Test3 {}
