package org.grails.plugins;

import grails.plugins.PluginFilter;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginFilterFactoryTests {

    @Test
    public void testIncludeFilterOne() {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter("one", null);
        assertTrue(bean instanceof IncludingPluginFilter);

        IncludingPluginFilter filter = (IncludingPluginFilter)bean;
        Set<String> suppliedNames = filter.getSuppliedNames();
        assertEquals(1, suppliedNames.size());
        assertTrue(suppliedNames.contains("one"));
    }

    @Test
    public void testIncludeFilter() {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter("one, two", " three , four ");
        assertTrue(bean instanceof IncludingPluginFilter);

        IncludingPluginFilter filter = (IncludingPluginFilter)bean;
        Set<String> suppliedNames = filter.getSuppliedNames();
        assertEquals(2, suppliedNames.size());
        assertTrue(suppliedNames.contains("two"));
    }

    @Test
    public void testExcludeFilter() {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter(null, " three , four ");
        assertTrue(bean instanceof ExcludingPluginFilter);

        ExcludingPluginFilter filter = (ExcludingPluginFilter)bean;
        Set<String> suppliedNames = filter.getSuppliedNames();
        assertEquals(2, suppliedNames.size());
        assertTrue(suppliedNames.contains("four"));
    }

    @Test
    public void testDefaultFilter() {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter(null, null);
        assertTrue(bean instanceof IdentityPluginFilter);
    }
}
