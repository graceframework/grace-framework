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
package grails.testing.services

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.util.ClassUtils

import grails.core.GrailsClass
import grails.gorm.services.Service
import grails.util.GrailsNameUtils

import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.core.exceptions.GrailsConfigurationException
import org.grails.testing.ParameterizedGrailsUnitTest

@CompileStatic
trait ServiceUnitTest<T> extends ParameterizedGrailsUnitTest<T> {

    public static final String DATATEST_CLASS = 'grails.testing.gorm.DataTest'

    private static Class dataTest
    private static boolean dataTestLoaded

    private void loadDataTestClass() {
        if (!dataTestLoaded) {
            try {
                dataTest = ClassUtils.forName(DATATEST_CLASS)
            }
            catch (ClassNotFoundException ignore) {
            }
        }
        dataTestLoaded = true
    }

    /**
     * Mocks a service class, registering it with the application context
     *
     * @param serviceClass The service class
     * @return An instance of the service
     */
    @CompileDynamic
    @Override
    void mockArtefact(Class<?> serviceClass) {
        try {
            GrailsClass serviceArtefact = grailsApplication.addArtefact(ServiceArtefactHandler.TYPE, serviceClass)

            defineBeans {
                "${serviceArtefact.propertyName}"(serviceClass) { bean ->
                    bean.autowire = true
                }
            }
        }
        catch (GrailsConfigurationException e) {
            if (serviceClass.getAnnotation(Service) != null) {
                loadDataTestClass()
                if (dataTest?.isAssignableFrom(this.class)) {
                    dataTest.getMethod('mockDataService', Class).invoke(this, serviceClass)
                }
                else {
                    throw new GrailsConfigurationException("Error attempting to test ${serviceClass.name}. "
                            + "Data services require gorm-testing-support to be on the classpath and the test to implement ${DATATEST_CLASS}")
                }
            }
            else {
                throw e
            }
        }
    }

    @Override
    String getBeanName(Class<?> serviceClass) {
        GrailsNameUtils.getPropertyName(serviceClass)
    }

    T getService() {
        getArtefactInstance()
    }

}
