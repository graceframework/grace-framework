package org.grails.spring

import org.junit.jupiter.api.Test
import org.springframework.core.io.DescriptiveResource

import static org.junit.jupiter.api.Assertions.*

class BeanConfigurationTests {
    @Test
    void testSetAbstract() {
        BeanConfiguration bc = new DefaultBeanConfiguration("demo")
        bc.setAbstract(true)

        bc.setResource(new DescriptiveResource("DemoResource"))

        def bd = bc.getBeanDefinition()

        assertTrue bd.abstract
        assertNotNull bd.resource
    }

    @Test
    void testSetBeanFactory() {
        BeanConfiguration bc = new DefaultBeanConfiguration("demo")
        bc.setFactoryBean("demoFactory")

        bc.setResource(new DescriptiveResource("DemoResource"))

        def bd = bc.getBeanDefinition()

        assertNotNull bd.resource
    }
}
