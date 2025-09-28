/*
 * Copyright 2013-2025 the original author or authors.
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
package grails.async.services

import java.lang.reflect.Method

import groovy.transform.CompileStatic
import org.springframework.beans.BeanWrapper
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.util.ReflectionUtils

import grails.async.decorator.PromiseDecorator
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

/**
 * A {@link PromiseDecorator} that wraps a {@link grails.async.Promise} in a transaction
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.3
 */
@CompileStatic
class TransactionalPromiseDecorator implements PromiseDecorator, TransactionDefinition {

    PlatformTransactionManager transactionManager
    @Delegate DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition()

    TransactionalPromiseDecorator(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager
    }

    TransactionalPromiseDecorator(PlatformTransactionManager transactionManager, DefaultTransactionDefinition transactionDefinition) {
        this.transactionManager = transactionManager
        this.transactionDefinition = transactionDefinition
    }

    TransactionalPromiseDecorator(PlatformTransactionManager transactionManager, Transactional transactionDefinition) {
        this.transactionManager = transactionManager
        TransactionDefinition definition = new DefaultTransactionDefinition()
        Method[] annotationProperties = transactionDefinition.annotationType().getDeclaredMethods()
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(definition)
        for (Method annotationProperty : annotationProperties) {
            String propertyName = annotationProperty.getName()
            if (bw.isWritableProperty(propertyName)) {
                Object value = ReflectionUtils.invokeMethod(annotationProperty, transactionDefinition)
                bw.setPropertyValue(propertyName, value)
            }
        }
        this.transactionDefinition = definition
    }

    @Override
    <D> Closure<D> decorate(Closure<D> original) {
        if (this.transactionManager != null) {
            return (Closure<D>){ args ->
                TransactionTemplate transactionTemplate = this.transactionDefinition != null ?
                        new TransactionTemplate(this.transactionManager, this.transactionDefinition) : new TransactionTemplate(this.transactionManager)
                transactionTemplate.execute({
                    original.call(args)
                } as TransactionCallback)
            }
        }
        return original
    }
}
