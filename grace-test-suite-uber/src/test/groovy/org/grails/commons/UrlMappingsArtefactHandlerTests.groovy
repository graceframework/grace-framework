package org.grails.commons

import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Tests for UrlMappingsArtefactHandler.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
class UrlMappingsArtefactHandlerTests {

    def mappingScript = '''
@grails.artefact.Artefact("UrlMappings")
class MyUrlMappings {
    static mappings = {
        "/$id/$year?/$month?/$day?" {
            controller = "blog"
            action = "show"
            constraints {
                year(matches:/\\d{4}/)
                month(matches:/\\d{2}/)
            }
        }

        "/product/$name" {
            controller = "product"
            action = "show"
        }
    }
}
'''

    @Test
    void testUrlMappingsArtefactHandler() {
        def gcl = new GroovyClassLoader()
        Class mappings = gcl.parseClass(mappingScript)
        def handler = new UrlMappingsArtefactHandler()

        assertTrue handler.isArtefactClass(mappings)
        assertNotNull handler.newArtefactClass(mappings)
    }
}
