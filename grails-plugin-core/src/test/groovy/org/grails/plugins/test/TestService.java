package org.grails.plugins.test;

import org.springframework.stereotype.Service;

@Service
public class TestService {
    private String name;

    public TestService() {
        this.name = "Test";
    }

    public TestService(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
