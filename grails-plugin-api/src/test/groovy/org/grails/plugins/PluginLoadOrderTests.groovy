package org.grails.plugins

import grails.plugins.DefaultGrailsPluginManager
import org.junit.jupiter.api.Test

import grails.plugins.GrailsPlugin

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class PluginLoadOrderTests {

    @Test
    void testPluginLoadBeforeAfter() {
        def gcl = new GroovyClassLoader()

        gcl.parseClass '''
class FiveGrailsPlugin {
    def version = 0.1
    def loadAfter = ['one']
}

class FourGrailsPlugin {
    def version = 0.1
    def loadAfter = ['two']
}
class OneGrailsPlugin {
    def version = 0.1
    def loadBefore = ['two']
}
class TwoGrailsPlugin {
    def version = 0.1
    def loadAfter = ['three']
}
class ThreeGrailsPlugin {
    def version = 0.1
}
'''

        def one = gcl.loadClass("OneGrailsPlugin")
        def two = gcl.loadClass("TwoGrailsPlugin")
        def three = gcl.loadClass("ThreeGrailsPlugin")
        def four = gcl.loadClass("FourGrailsPlugin")
        def five = gcl.loadClass("FiveGrailsPlugin")
        def pluginManager = new DefaultGrailsPluginManager([one,two,three, four,five] as Class[],
            new MockGrailsApplication())

        pluginManager.loadCorePlugins = false
        pluginManager.loadPlugins()

        List<GrailsPlugin> pluginList = List.of(pluginManager.getAllPlugins());

        assertEquals(5, pluginList.size())

        int orderOne = getOrderOfPlugin(pluginList, 'one')
        int orderTwo = getOrderOfPlugin(pluginList, 'two')
        int orderThree = getOrderOfPlugin(pluginList, 'three')
        int orderFour = getOrderOfPlugin(pluginList, 'four')
        int orderFive = getOrderOfPlugin(pluginList, 'five')
        // Plugin loaded order not Fixed
        assertTrue orderOne < orderTwo, 'One should load before Two'
        assertTrue orderThree < orderTwo, 'Three should load before Two'
        assertTrue orderFour > orderTwo, 'Four should load after Two'
        assertTrue orderFive > orderOne, 'Five should load after Two'
    }

    def getOrderOfPlugin(List<GrailsPlugin> pluginList, String name) {
        for (int i = 0; i < pluginList.size(); i++) {
            if (pluginList[i].name == name) {
                return i
            }
        }
        return -1
    }
}
