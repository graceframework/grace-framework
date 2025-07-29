/*
 * Copyright 2016-2023 the original author or authors.
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
package grails.testing.web.controllers

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import javassist.util.proxy.ProxyFactory

import grails.testing.web.GrailsWebUnitTest
import grails.web.mime.MimeType

import org.grails.testing.ParameterizedGrailsUnitTest
import org.grails.testing.runtime.support.ActionSettingMethodHandler
import org.grails.web.pages.GroovyPagesUriSupport
import org.grails.web.util.GrailsApplicationAttributes

@CompileStatic
trait ControllerUnitTest<T> implements ParameterizedGrailsUnitTest<T>, GrailsWebUnitTest {

    static final String FORM_CONTENT_TYPE = MimeType.FORM.name
    static final String MULTIPART_FORM_CONTENT_TYPE = MimeType.MULTIPART_FORM.name
    static final String ALL_CONTENT_TYPE = MimeType.ALL.name
    static final String HTML_CONTENT_TYPE = MimeType.HTML.name
    static final String XHTML_CONTENT_TYPE = MimeType.XHTML.name
    static final String XML_CONTENT_TYPE = MimeType.XML.name
    static final String JSON_CONTENT_TYPE = MimeType.JSON.name
    static final String TEXT_XML_CONTENT_TYPE = MimeType.TEXT_XML.name
    static final String TEXT_JSON_CONTENT_TYPE = MimeType.TEXT_JSON.name
    static final String HAL_JSON_CONTENT_TYPE = MimeType.HAL_JSON.name
    static final String HAL_XML_CONTENT_TYPE = MimeType.HAL_XML.name
    static final String ATOM_XML_CONTENT_TYPE = MimeType.ATOM_XML.name

    private T proxyInstance

    /**
     * @return The model of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map getModel() {
        Map model = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)?.modelAndView?.model
        if (model == null) {
            model = request.getAttribute(GrailsApplicationAttributes.TEMPLATE_MODEL)
        }
        model ?: [:]
    }

    /**
     * @return The view of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    String getView() {
        def controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)

        def viewName = controller?.modelAndView?.viewName
        if (viewName != null) {
            return viewName
        }

        if (webRequest.controllerName && webRequest.actionName) {
            new GroovyPagesUriSupport().getViewURI(webRequest.controllerName, webRequest.actionName)
        }
        else {
            return null
        }
    }

    /**
     * Mocks a Grails controller class, providing the needed behavior and defining it in the ApplicationContext
     *
     * @param controllerClass The controller class
     * @return An instance of the controller
     */
    @Override
    void mockArtefact(Class<?> controllerClass) {
        mockController(controllerClass)
    }

    @Override
    String getBeanName(Class<?> controllerClass) {
        controllerClass.name
    }

    boolean disableControllerProxy() {
        false
    }

    T getController() {
        if (disableControllerProxy()) {
            getArtefactInstance()
        }
        else {
            if (this.proxyInstance == null) {
                T artefact = getArtefactInstance()
                ProxyFactory factory = new ProxyFactory()
                factory.setSuperclass(getTypeUnderTest())
                this.proxyInstance = (T) factory.create(new Class<?>[0], new Object[0], new ActionSettingMethodHandler(artefact, getWebRequest()))
            }
            this.proxyInstance
        }
    }

}
