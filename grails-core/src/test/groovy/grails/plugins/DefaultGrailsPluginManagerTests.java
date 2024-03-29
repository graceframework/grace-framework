package grails.plugins;

import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import groovy.lang.GroovyClassLoader;
import org.grails.plugins.IncludingPluginFilter;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultGrailsPluginManagerTests {

    private Class<?> first;
    private Class<?> second;
    private Class<?> third;
    private Class<?> fourth;

    @Test
    public void testLoadPlugins() {

        GroovyClassLoader gcl = new GroovyClassLoader();

        first = gcl.parseClass("class FirstGrailsPlugin {\n" +
            "def version = 1.0\n" +
            "}");
        second = gcl.parseClass("class SecondGrailsPlugin {\n" +
            "def version = 1.0\n" +
            "def dependsOn = [first:version]\n" +
            "}");
        third = gcl.parseClass("import grails.util.GrailsUtil\n" +
                "class ThirdGrailsPlugin {\n" +
            "def version = GrailsUtil.getGrailsVersion()\n" +
            "def dependsOn = [i18n:version]\n" +
            "}");
        fourth = gcl.parseClass("class FourthGrailsPlugin {\n" +
            "def version = 1.0\n" +
            "def dependsOn = [second:version, third:version]\n" +
            "}");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl);
        GenericApplicationContext parent = new GenericApplicationContext();
        parent.getDefaultListableBeanFactory().registerSingleton(GrailsApplication.APPLICATION_ID, app);

        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[]{first, second, third, fourth}, app);
        manager.setParentApplicationContext(parent);
        manager.setLoadCorePlugins(false);
        manager.setPluginFilter(new IncludingPluginFilter("first", "third"));

        manager.loadPlugins();

        List<GrailsPlugin> pluginList = List.of(manager.getAllPlugins());

        assertNotNull(manager.getGrailsPlugin("first"));
        assertNull(manager.getGrailsPlugin("third"));

        assertEquals(1, pluginList.size(), "Expected plugins not loaded. Expected " + 1 + " but got " + pluginList);
    }

    /**
     * Test the known 1.0.2 failure where:
     *
     * mail 0.3 = has no deps
     * quartz 0.3-SNAPSHOT: loadAfter = ['core', 'hibernate']
     * emailconfirmation 0.4: dependsOn = [quartz:'0.3 > *', mail: '0.2 > *']
     *
     * ...and emailconfirmation is NOT loaded first.
     */
    @Test
    public void testDependenciesWithDelayedLoadingWithVersionRangeStrings() {
        GroovyClassLoader gcl = new GroovyClassLoader();

        // These are defined in a specific order so that the one with the range dependencies
        // is the first in the list, and its dependencies load after
        first = gcl.parseClass("class FirstGrailsPlugin {\n" +
            "def version = \"0.4\"\n" +
            "def dependsOn = [second:'0.3 > *', third:'0.2 > *']\n" +
            "}");
        second = gcl.parseClass("class SecondGrailsPlugin {\n" +
            "def version = \"0.3\"\n" +
            "def dependsOn = [:]\n" +
            "}");
        third = gcl.parseClass("class ThirdGrailsPlugin {\n" +
            "def version = \"0.3-SNAPSHOT\"\n" +
            "def loadAfter = ['core', 'hibernate']\n" +
            "}");

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl);
        GenericApplicationContext parent = new GenericApplicationContext();
        parent.getDefaultListableBeanFactory().registerSingleton(GrailsApplication.APPLICATION_ID, app);

        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[]{first, second, third}, app);
        manager.setParentApplicationContext(parent);
        manager.setLoadCorePlugins(false);
        manager.setPluginFilter(new IncludingPluginFilter("dataSource", "first", "second", "third"));

        manager.loadPlugins();

        List<GrailsPlugin> pluginList = List.of(manager.getAllPlugins());

        assertNotNull(manager.getGrailsPlugin("first"));
        assertNotNull(manager.getGrailsPlugin("second"));
        assertNotNull(manager.getGrailsPlugin("third"));

        assertEquals(3, pluginList.size(), "Expected plugins not loaded. Expected " + 3 + " but got " + pluginList);
    }

    DefaultGrailsPluginManager loadPlugins(String firstClassString, String secondClassString, String thirdClassString, String fourthClassString) {
        GroovyClassLoader gcl = new GroovyClassLoader();

        first = gcl.parseClass(firstClassString);
        second = gcl.parseClass(secondClassString);
        third = gcl.parseClass(thirdClassString);
        fourth = gcl.parseClass(fourthClassString);

        GrailsApplication app = new DefaultGrailsApplication(new Class[]{}, gcl);
        GenericApplicationContext parent = new GenericApplicationContext();
        parent.getDefaultListableBeanFactory().registerSingleton(GrailsApplication.APPLICATION_ID, app);

        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(new Class[]{first, second, third, fourth}, app);
        manager.setParentApplicationContext(parent);
        manager.setLoadCorePlugins(false);
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
}
