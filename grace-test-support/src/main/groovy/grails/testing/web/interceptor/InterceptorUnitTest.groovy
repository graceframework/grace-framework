/*
 * Copyright 2016-2022 the original author or authors.
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
package grails.testing.web.interceptor

import java.lang.reflect.ParameterizedType

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.springframework.web.servlet.ModelAndView

import grails.artefact.Interceptor
import grails.core.GrailsClass
import grails.testing.web.GrailsWebUnitTest
import grails.util.GrailsNameUtils
import grails.web.mapping.UrlMappingInfo

import org.grails.plugins.web.interceptors.GrailsInterceptorHandlerInterceptorAdapter
import org.grails.plugins.web.interceptors.InterceptorArtefactHandler
import org.grails.testing.ParameterizedGrailsUnitTest
import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.util.GrailsApplicationAttributes

@CompileStatic
trait InterceptorUnitTest<T> implements ParameterizedGrailsUnitTest<T>, GrailsWebUnitTest {

    private boolean hasBeenMocked = false

    /**
     * Mock the interceptor for the given name
     *
     * @param interceptorClass The interceptor class
     * @return The mocked interceptor
     */
    @CompileDynamic
    Interceptor mockInterceptor(Class<?> interceptorClass) {
        GrailsClass artefact = grailsApplication.addArtefact(InterceptorArtefactHandler.TYPE, interceptorClass)
        defineBeans {
            "${artefact.propertyName}"(artefact.clazz) { bean ->
                bean.autowire = true
            }
        }
        getHandlerInterceptor()
                .setInterceptors(applicationContext.getBeansOfType(Interceptor).values() as Interceptor[])
        (Interceptor) applicationContext.getBean(artefact.propertyName, interceptorClass)
    }

    /**
     * Execute the given request with the registered interceptors
     *
     * @param arguments The arguments
     * @param callable A callable containing an invocation of a controller action
     * @return The result of the callable execution
     */
    Object withInterceptors(Map<String, Object> arguments, Closure callable) {
        ensureInterceptorHasBeenMocked()
        UrlMappingInfo info = withRequest(arguments)

        def hi = getHandlerInterceptor()

        try {
            if (hi.preHandle(request, response, this)) {
                def result = callable.call()
                ModelAndView modelAndView = null
                def modelAndViewObject = request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                if (modelAndViewObject instanceof ModelAndView) {
                    modelAndView = (ModelAndView) modelAndViewObject
                }
                else if (result instanceof Map) {
                    modelAndView = new ModelAndView(info?.actionName ?: 'index', new HashMap<String, Object>((Map) result))
                }
                else if (result instanceof ModelAndView) {
                    return (ModelAndView) result
                }
                hi.postHandle(request, response, this, modelAndView)
                return result
            }
        }
        catch (Exception e) {
            hi.afterCompletion(request, response, this, e)
        }
    }

    /**
     * Allows testing of the interceptor directly by setting up an incoming request that can be matched prior to invoking the
     * interceptor
     *
     * @param arguments Named arguments specifying the controller/action or URI that interceptor should match
     *
     * @return The {@link UrlMappingInfo} object
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    UrlMappingInfo withRequest(Map<String, Object> arguments) {
        ensureInterceptorHasBeenMocked()
        UrlMappingInfo info = null
        if (arguments.uri) {
            request.requestURI = arguments.uri.toString()
        }
        else {
            info = new ForwardUrlMappingInfo(arguments)
            request.setAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST, info)
        }

        for (String name in request.attributeNames.findAll { String n -> n.endsWith(InterceptorArtefactHandler.MATCH_SUFFIX) }) {
            request.removeAttribute(name)
        }
        info
    }

    private GrailsInterceptorHandlerInterceptorAdapter getHandlerInterceptor() {
        applicationContext.getBean(GrailsInterceptorHandlerInterceptorAdapter)
    }

    void mockArtefact(Class<?> interceptorClass) {
        mockInterceptor((Class<? extends Interceptor>) interceptorClass)
    }

    String getBeanName(Class<?> interceptorClass) {
        GrailsNameUtils.getPropertyName(interceptorClass)
    }

    private Class<T> getInterceptorTypeUnderTest() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().genericInterfaces.find { genericInterface ->
            genericInterface instanceof ParameterizedType &&
                    InterceptorUnitTest.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
        }

        parameterizedType?.actualTypeArguments[0]
    }

    T getInterceptor() {
        ensureInterceptorHasBeenMocked()
        getArtefactInstance()
    }

    private void ensureInterceptorHasBeenMocked() {
        if (!hasBeenMocked) {
            mockInterceptor getInterceptorTypeUnderTest()
            hasBeenMocked = true
        }
    }

}
