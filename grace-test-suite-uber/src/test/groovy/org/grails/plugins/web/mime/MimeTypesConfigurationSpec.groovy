package org.grails.plugins.web.mime

import grails.web.mime.MimeType
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class MimeTypesConfigurationSpec extends Specification implements GrailsUnitTest {

    void "test when no mimeTypes configured then default should be used"() {
        when:
        MimeType[] mimeTypes = applicationContext.getBean(MimeType[])

        then:
        MimeType.createDefaults() == mimeTypes
    }
}
