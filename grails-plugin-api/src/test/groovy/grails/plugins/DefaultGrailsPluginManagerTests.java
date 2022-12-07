package grails.plugins;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.GroovyClassLoader;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import grails.core.GrailsApplication;

import org.grails.plugins.IncludingPluginFilter;
import org.grails.plugins.MockGrailsApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultGrailsPluginManagerTests {

    DefaultGrailsPluginManager loadPlugins(String firstClassString, String secondClassString, String thirdClassString, String fourthClassString) {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class<?> first = gcl.parseClass(firstClassString);
        Class<?> second = gcl.parseClass(secondClassString);
        Class<?> third = gcl.parseClass(thirdClassString);
        Class<?> fourth = gcl.parseClass(fourthClassString);

        GrailsApplication app = new MockGrailsApplication(new Class[] {}, gcl);
        GenericApplicationContext parent = new GenericApplicationContext();
        parent.getDefaultListableBeanFactory().registerSingleton(GrailsApplication.APPLICATION_ID, app);

        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[] { first, second, third, fourth }, app);
        manager.setParentApplicationContext(parent);
        manager.setPluginFilter(new IncludingPluginFilter("first", "second", "third", "fourth"));

        manager.loadPlugins();
        return manager;
    }

    @Test
    public void testLoadingOrderLoadBeforeAndLoadAfter() {
        DefaultGrailsPluginManager manager = loadPlugins("class FirstGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "def loadAfter = ['second', 'third']\n" +
                "}", "class SecondGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "}", "import grails.util.GrailsUtil\n" +
                "class ThirdGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "def loadBefore = ['fourth']\n" +
                "}", "class FourthGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "def loadBefore = ['first', 'second']\n" +
                "}");

        List<GrailsPlugin> pluginList = List.of(manager.getAllPlugins());

        List<GrailsPlugin> expectedOrder = new ArrayList<>();
        expectedOrder.add(manager.getGrailsPlugin("third"));
        expectedOrder.add(manager.getGrailsPlugin("fourth"));
        expectedOrder.add(manager.getGrailsPlugin("second"));
        expectedOrder.add(manager.getGrailsPlugin("first"));

        assertEquals(expectedOrder, pluginList, "Expected plugin order");
    }

    @Test
    public void testLoadingOrderPluginsOrdered() {
        DefaultGrailsPluginManager manager = loadPlugins("class FirstGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "def order = 5\n" +
                "}", "@javax.annotation.Priority(3) class SecondGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "}", "@org.springframework.core.annotation.Order(7) class ThirdGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "}", "class FourthGrailsPlugin implements org.springframework.core.PriorityOrdered {\n" +
                "def version = '1.0'\n" +
                "int getOrder() { 1 }\n" +
                "}");

        List<GrailsPlugin> pluginList = manager.getPluginList();

        List<GrailsPlugin> expectedOrder = new ArrayList<>();
        expectedOrder.add(manager.getGrailsPlugin("fourth"));
        expectedOrder.add(manager.getGrailsPlugin("second"));
        expectedOrder.add(manager.getGrailsPlugin("first"));
        expectedOrder.add(manager.getGrailsPlugin("third"));

        assertEquals(expectedOrder, pluginList, "Expected plugin order by ordered");
        assertEquals(1, manager.getGrailsPlugin("fourth").getOrder(), "Expected fourth plugin order is 1");
        assertEquals(3, manager.getGrailsPlugin("second").getOrder(), "Expected second plugin order is 3");
        assertEquals(5, manager.getGrailsPlugin("first").getOrder(), "Expected first plugin order is 5");
        assertEquals(7, manager.getGrailsPlugin("third").getOrder(), "Expected third plugin is 7");
    }

    @Test
    public void testAddUserPlugin() {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class<?> first = gcl.parseClass("class FirstGrailsPlugin {\n" +
                "def version = '1.0'\n" +
                "}");

        GrailsApplication app = new MockGrailsApplication(new Class[] {}, gcl);
        GenericApplicationContext parent = new GenericApplicationContext();
        parent.getDefaultListableBeanFactory().registerSingleton(GrailsApplication.APPLICATION_ID, app);

        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[] {}, app);

        manager.addUserPlugin(first);
        manager.loadPlugins();

        assertEquals(1, manager.getUserPlugins().length);
    }

}
