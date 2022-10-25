package org.grails.testing.gorm.spock

import grails.testing.gorm.DataTest
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.builtin.UniqueConstraint
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.reflect.ClassUtils
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.validation.ConstraintEvalUtils
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.ConfigurableConversionService

@CompileStatic
class DataTestSetupSpecInterceptor implements IMethodInterceptor {

    public static Boolean IS_OLD_SETUP = false
    public static final BEAN_NAME = "validateableConstraintsEvaluator"
    private static Class constraintsEvaluator = DefaultConstraintEvaluator

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        configureDataTest((DataTest)invocation.instance)
        invocation.proceed()
    }

    @CompileDynamic
    void setupDataTestBeans(DataTest testInstance) {

        testInstance.defineBeans {
            ConfigurableConversionService conversionService = application.mainContext.getEnvironment().getConversionService()
            conversionService.addConverter(new Converter<String, Class>() {
                @Override
                Class convert(String source) {
                    Class.forName(source)
                }
            })
            grailsDatastore SimpleMapDatastore, DatastoreUtils.createPropertyResolver(application.config), application.config.dataSources.keySet(), testInstance.domainClassesToMock?: [] as Class<?>[]

                constraintRegistry(DefaultConstraintRegistry, ref("messageSource"))
                grailsDomainClassMappingContext(grailsDatastore: "getMappingContext")

                "${BEAN_NAME}"(constraintsEvaluator, constraintRegistry, grailsDomainClassMappingContext, ConstraintEvalUtils.getDefaultConstraints(application.config))

            transactionManager(DatastoreTransactionManager) {
                datastore = ref('grailsDatastore')
            }
        }

        if (!IS_OLD_SETUP) {
            testInstance.grailsApplication.setMappingContext(
                    testInstance.applicationContext.getBean('grailsDomainClassMappingContext', MappingContext)
            )
        }
    }

    void configureDataTest(DataTest testInstance) {
        setupDataTestBeans testInstance
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)testInstance.applicationContext
        applicationContext.getBean('constraintRegistry', ConstraintRegistry).addConstraint(UniqueConstraint)

        if (!testInstance.domainsHaveBeenMocked) {
            def classes = testInstance.domainClassesToMock
            if (classes) {
                testInstance.mockDomains classes
            }
            testInstance.domainsHaveBeenMocked = true
        }
    }

}
