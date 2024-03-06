package org.grails.plugins.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {
    @Bean
    public TestBean testBean() {
        return new TestBean();
    }

    static class TestBean {
        private String name;

        public TestBean() {
            this.name = "Test";
        }
    }
}
