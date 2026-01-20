/*
 * Copyright 2021-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.io.support

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import spock.lang.Specification

import grails.util.BuildSettings

class GrailsResourceUtilsSpec extends Specification {

    private static final String TEST_URL = "file:///test/grails/app/grails-app/domain/Test.groovy"
    private static final String TEST_PACKAGE_URL = "file:///test/grails/app/grails-app/domain/mycompany/Test.groovy"
    private static final String TEST_CONTROLLER_URL = "file:///test/grails/app/grails-app/controllers/TestController.groovy"
    private static final String TEST_PLUGIN_CTRL = "file:///test/grails/app/plugins/myplugin/grails-app/controllers/TestController.groovy"

    private static final String WEBINF_CONTROLLER = "file:///test/grails/app/WEB-INF/grails-app/controllers/TestController.groovy"
    private static final String WEBINF_PLUGIN_CTRL = "file:///test/grails/app/WEB-INF/plugins/myplugin/grails-app/controllers/TestController.groovy"

    private static final String UNIT_TESTS_URL = "file:///test/grails/app/grails-tests/SomeTests.groovy"

    void testGetArtifactDirectory() {
        expect:
        GrailsResourceUtils.getArtefactDirectory(TEST_CONTROLLER_URL) == "controllers"
        GrailsResourceUtils.getArtefactDirectory(TEST_PACKAGE_URL) == "domain"
    }

    void testJavaAndGroovySources() {
        expect:
        GrailsResourceUtils.getClassName(TEST_PACKAGE_URL) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/domain/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blahblah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah-blah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah--blah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah_blah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blahblah/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah-blah/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah--blah/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/grails-app/blah_blah/mycompany/Test.java").getPath()) == "mycompany.Test"

        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/groovy/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/groovy/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/java/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/java/mycompany/Test.java").getPath()) == "mycompany.Test"

        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/groovy/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/groovy/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/java/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/java/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/groovy/mycompany/Test.groovy").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/groovy/mycompany/Test.groovy").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/main/java/mycompany/Test.groovy").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/app/src/test/java/mycompany/Test.groovy").getPath()) == "mycompany.Test"

        GrailsResourceUtils.getClassName("file:///test/grails/myapp/app/domain/mycompany/Test.groovy") == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/domain/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blahblah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blah-blah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blah--blah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blah_blah/Test.java").getPath()) == "Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blahblah/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blah-blah/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blah--blah/mycompany/Test.java").getPath()) == "mycompany.Test"
        GrailsResourceUtils.getClassName(new File("/test/grails/myapp/app/blah_blah/mycompany/Test.java").getPath()) == "mycompany.Test"
    }

    void testIsDomainClass() {
        expect:
        GrailsResourceUtils.isDomainClass(new URL("file:///test/grails/myapp/grails-app/domain/Test.groovy"))
        GrailsResourceUtils.isDomainClass(new URL("file:///test/grails/myapp/app/domain/Test.groovy"))
    }

    void testGetPathFromRoot() {
        expect:
        GrailsResourceUtils.getPathFromRoot(TEST_PACKAGE_URL) == "mycompany/Test.groovy"
        GrailsResourceUtils.getPathFromRoot(TEST_URL) == "Test.groovy"
        GrailsResourceUtils.getPathFromRoot("file:///test/grails/myapp/app/domain/mycompany/Test.groovy") == "mycompany/Test.groovy"
        GrailsResourceUtils.getPathFromRoot("file:///test/grails/myapp/app/domain/Test.groovy") == "Test.groovy"
    }

    void testGetClassNameResource() {
        expect:
        GrailsResourceUtils.getClassName(new UrlResource(new URL("file:///test/grails/myapp/grails-app/domain/Test.groovy"))) == "Test"
        GrailsResourceUtils.getClassName(new UrlResource(new URL("file:///test/grails/myapp/app/domain/Test.groovy"))) == "Test"
    }

    void testGetClassNameString() {
        expect:
        GrailsResourceUtils.getClassName("file:///test/grails/myapp/grails-app/domain/Test.groovy") == "Test"
        GrailsResourceUtils.getClassName("file:///test/grails/myapp/app/domain/Test.groovy") == "Test"
    }

    void testIsGrailsPath() {
        expect:
        GrailsResourceUtils.isGrailsPath("file:///test/grails/myapp/grails-app/domain/Test.groovy")
        GrailsResourceUtils.isGrailsPath("file:///test/grails/myapp/grails-app/init/Application.groovy")
        GrailsResourceUtils.isGrailsPath("file:///test/grails/myapp/grails-app/conf/spring/resources.groovy")
        GrailsResourceUtils.isGrailsPath("file:///test/grails/myapp/app/domain/Test.groovy")
        GrailsResourceUtils.isGrailsPath("file:///test/grails/myapp/app/init/Application.groovy")
        GrailsResourceUtils.isGrailsPath("file:///test/grails/myapp/app/conf/spring/resources.groovy")
    }

    void testIsGrailsResource() {
        expect:
        GrailsResourceUtils.isGrailsResource(new UrlResource(new URL("file:///test/grails/myapp/grails-app/domain/Test.groovy")))
        GrailsResourceUtils.isGrailsResource(new UrlResource(new URL("file:///test/grails/myapp/grails-app/init/Application.groovy")))
        GrailsResourceUtils.isGrailsResource(new UrlResource(new URL("file:///test/grails/myapp/grails-app/conf/spring/resources.groovy")))
        GrailsResourceUtils.isGrailsResource(new UrlResource(new URL("file:///test/grails/myapp/app/domain/Test.groovy")))
        GrailsResourceUtils.isGrailsResource(new UrlResource(new URL("file:///test/grails/myapp/app/init/Application.groovy")))
        GrailsResourceUtils.isGrailsResource(new UrlResource(new URL("file:///test/grails/myapp/app/conf/spring/resources.groovy")))
    }

    void testIsProjectSource() {
        expect:
        GrailsResourceUtils.isProjectSource("file:///test/grails/myapp/grails-app/domain/Test.groovy")
        GrailsResourceUtils.isProjectSource("file:///test/grails/myapp/grails-app/init/Application.groovy")
        GrailsResourceUtils.isProjectSource("file:///test/grails/myapp/grails-app/conf/spring/resources.groovy")
        GrailsResourceUtils.isProjectSource("file:///test/grails/myapp/app/domain/Test.groovy")
        GrailsResourceUtils.isProjectSource("file:///test/grails/myapp/app/init/Application.groovy")
        GrailsResourceUtils.isProjectSource("file:///test/grails/myapp/app/conf/spring/resources.groovy")
    }

    void testIsTestPath() {
        expect:
        GrailsResourceUtils.isGrailsPath(UNIT_TESTS_URL)
    }

    void testGetTestNameResource() {
        when:
        Resource r = new UrlResource(new URL(UNIT_TESTS_URL))

        then:
        GrailsResourceUtils.getClassName(r) == "SomeTests"
    }

    void testGetTestNameString() {
        expect:
        GrailsResourceUtils.getClassName(UNIT_TESTS_URL) == "SomeTests"
    }

    void testGetViewsDirForURL() {
        when:
        Resource viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_CONTROLLER_URL))

        then:
        viewsDir.getURL().toString().endsWith("/test/grails/app/grails-app/views")

        viewsDir == GrailsResourceUtils.getViewsDir(new UrlResource(TEST_URL))
        viewsDir.getURL().toString().endsWith("/test/grails/app/grails-app/views")
    }

    void testGetAppDir() {
        when:
        Resource appDir = GrailsResourceUtils.getAppDir(new UrlResource(TEST_CONTROLLER_URL))

        then:
        appDir.getURL().toString().endsWith("/test/grails/app/grails-app")
        appDir == GrailsResourceUtils.getAppDir(new UrlResource(TEST_URL))
        appDir.getURL().toString().endsWith("/test/grails/app/grails-app")
    }

    void testGetDirWithinWebInf() {
        when:
        Resource viewsDir = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_CONTROLLER_URL))
        Resource pluginViews = GrailsResourceUtils.getViewsDir(new UrlResource(TEST_PLUGIN_CTRL))

        Resource webInfViews = GrailsResourceUtils.getViewsDir(new UrlResource(WEBINF_CONTROLLER))
        Resource webInfPluginViews = GrailsResourceUtils.getViewsDir(new UrlResource(WEBINF_PLUGIN_CTRL))

        then:
        viewsDir.getURL().toString().endsWith("/test/grails/app/grails-app/views")
        pluginViews.getURL().toString().endsWith("/test/grails/app/plugins/myplugin/grails-app/views")
        webInfViews.getURL().toString().endsWith("/test/grails/app/WEB-INF/grails-app/views")
        webInfPluginViews.getURL().toString().endsWith("/test/grails/app/WEB-INF/plugins/myplugin/grails-app/views")

        GrailsResourceUtils.getRelativeInsideWebInf(webInfViews) == "/WEB-INF/grails-app/views"
        GrailsResourceUtils.getRelativeInsideWebInf(webInfPluginViews) == "/WEB-INF/plugins/myplugin/grails-app/views"

        GrailsResourceUtils.getRelativeInsideWebInf(pluginViews) == "/WEB-INF/plugins/myplugin/grails-app/views"
        GrailsResourceUtils.getRelativeInsideWebInf(viewsDir) == "/WEB-INF/grails-app/views"
    }

    void testGetPluginContextPath() {
        expect:
        GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(TEST_CONTROLLER_URL), null) == ""
        GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(TEST_PLUGIN_CTRL), null) == "plugins/myplugin"
        GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_CONTROLLER), null) == ""
        GrailsResourceUtils.getStaticResourcePathForResource(new UrlResource(WEBINF_PLUGIN_CTRL), null) == "plugins/myplugin"
    }

    void testAppendPiecesForUri() {
        expect:
        GrailsResourceUtils.appendPiecesForUri("") == ""
        GrailsResourceUtils.appendPiecesForUri("/alpha", "/beta", "/gamma") == "/alpha/beta/gamma"
        GrailsResourceUtils.appendPiecesForUri("/alpha/", "/beta/", "/gamma") == "/alpha/beta/gamma"
        GrailsResourceUtils.appendPiecesForUri("/alpha/", "/beta/", "/gamma/") == "/alpha/beta/gamma/"
        GrailsResourceUtils.appendPiecesForUri("alpha", "beta", "gamma") == "alpha/beta/gamma"
    }

    void testGetPathFromBaseDir() {
        expect:
        GrailsResourceUtils.getPathFromBaseDir("${BuildSettings.BASE_DIR.absolutePath}/grails-app/views/demo/index.gsp").endsWith(['grails-app', 'views', 'demo', 'index.gsp'].join(File.separator))
        GrailsResourceUtils.getPathFromBaseDir("${BuildSettings.BASE_DIR.absolutePath}/src/main/demo/index.gsp").endsWith(['src', 'main', 'demo', 'index.gsp'].join(File.separator))
        GrailsResourceUtils.getPathFromBaseDir("/alpha/index.gsp").endsWith(['alpha', 'index.gsp'].join(File.separator))
    }

}
