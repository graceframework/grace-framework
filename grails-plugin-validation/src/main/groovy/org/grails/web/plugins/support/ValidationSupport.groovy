/*
 * Copyright 2011-2022 the original author or authors.
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
package org.grails.web.plugins.support

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.validation.FieldError

import grails.gorm.validation.ConstrainedProperty
import grails.util.Holders
import grails.validation.Constrained
import grails.validation.ConstrainedDelegate
import grails.validation.ValidationErrors

import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator

class ValidationSupport {

    static BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper()

    static boolean validateInstance(object, List fieldsToValidate = null) {
        beforeValidateHelper.invokeBeforeValidate(object, fieldsToValidate)

        if (!object.hasProperty('constraints')) {
            return true
        }

        Map<String, Constrained> constraints = getObjectConstraints(object)
        if (constraints) {
            def localErrors = new ValidationErrors(object, object.class.name)
            def originalErrors = object.errors
            for (originalError in originalErrors.allErrors) {
                if (originalError instanceof FieldError) {
                    if (originalErrors.getFieldError(originalError.field)?.bindingFailure) {
                        localErrors.addError originalError
                    }
                }
                else {
                    localErrors.addError originalError
                }
            }
            for (prop in constraints.values()) {
                if (fieldsToValidate == null || fieldsToValidate.contains(prop.propertyName)) {
                    def fieldError = originalErrors.getFieldError(prop.propertyName)
                    if (fieldError == null || !fieldError.bindingFailure) {
                        prop.validate(object, object.getProperty(prop.propertyName), localErrors)
                    }
                }
            }
            object.errors = localErrors
        }

        !object.errors.hasErrors()
    }

    @CompileDynamic
    private static Map<String, Constrained> getObjectConstraints(object) {
        object.constraints
    }

    @CompileStatic
    static Map<String, Constrained> getConstrainedPropertiesForClass(Class<?> clazz, boolean defaultNullable = false) {
        BeanFactory ctx = Holders.findApplicationContext()

        ConstraintsEvaluator evaluator
        if (ctx != null) {
            try {
                evaluator = ctx.getBean(ConstraintsEvaluator)
            }
            catch (NoSuchBeanDefinitionException e) {
                evaluator = new DefaultConstraintEvaluator()
            }
        }
        else {
            evaluator = new DefaultConstraintEvaluator()
        }

        Map<String, ConstrainedProperty> evaluatedConstraints = evaluator.evaluate(clazz, defaultNullable)
        Map<String, Constrained> finalConstraints = [:]
        for (entry in evaluatedConstraints) {
            finalConstraints.put(entry.key, new ConstrainedDelegate(entry.value))
        }
        finalConstraints
    }

}
