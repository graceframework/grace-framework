package org.grails.plugins

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.Resource

class TestBinaryGrailsPlugin {
    def version = 1.0
}

class MockBinaryPluginResource extends ByteArrayResource {

    Map<String, Resource> relativesResources = [:]

    MockBinaryPluginResource(byte[] byteArray) {
        super(byteArray)
    }

    @Override
    Resource createRelative(String relativePath) {
        return relativesResources[relativePath]
    }
}