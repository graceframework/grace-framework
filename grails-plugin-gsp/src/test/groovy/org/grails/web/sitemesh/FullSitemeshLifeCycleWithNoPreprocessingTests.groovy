package org.grails.web.sitemesh

import com.opensymphony.module.sitemesh.RequestConstants
import org.junit.jupiter.api.BeforeEach

class FullSitemeshLifeCycleWithNoPreprocessingTests extends FullSitemeshLifeCycleTests {

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp()
        def config = new ConfigSlurper().parse('''
grails.views.gsp.sitemesh.preprocess=false
''')
        buildMockRequest(config)
        def page = new GSPSitemeshPage()
        request.setAttribute(RequestConstants.PAGE, page)
        request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, page)
    }

    @Override
    void testMultipleLevelsOfLayouts() {
        // no-op
    }
}
