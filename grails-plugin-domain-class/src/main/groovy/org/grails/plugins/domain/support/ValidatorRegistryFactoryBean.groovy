package org.grails.plugins.domain.support

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.validation.ValidatorRegistry
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy

/**
 * A factory bean for the validator registry
 *
 * @author Graeme Rocher
 * @since 3.3
 */
@CompileStatic
class ValidatorRegistryFactoryBean implements FactoryBean<ValidatorRegistry> {

    @Lazy
    @Autowired
    @Qualifier('grailsDomainClassMappingContext')
    MappingContext mappingContext

    @Override
    ValidatorRegistry getObject() throws Exception {
        mappingContext.validatorRegistry
    }

    @Override
    Class<?> getObjectType() {
        ValidatorRegistry
    }

    @Override
    boolean isSingleton() {
        true
    }

}
