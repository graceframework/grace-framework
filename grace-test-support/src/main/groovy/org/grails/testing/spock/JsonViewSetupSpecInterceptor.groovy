package org.grails.testing.spock

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.springframework.web.servlet.i18n.SessionLocaleResolver

import grails.core.GrailsApplication
import grails.testing.views.json.JsonViewUnitTest
import grails.views.mvc.GenericGroovyTemplateViewResolver
import grails.views.resolve.PluginAwareTemplateResolver

import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.views.json.JsonViewConfiguration
import org.grails.views.json.JsonViewTemplateEngine
import org.grails.views.json.api.jsonapi.DefaultJsonApiIdRenderer
import org.grails.views.json.mvc.JsonViewResolver
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.UrlMappingsHolderFactoryBean

@CompileStatic
class JsonViewSetupSpecInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        JsonViewUnitTest test = (JsonViewUnitTest) invocation.instance
        setup(test)
        invocation.proceed()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void setup(JsonViewUnitTest test) {

        GrailsApplication grailsApplication = test.grailsApplication
        def config = grailsApplication.config

        test.defineBeans {
            grailsLinkGenerator(DefaultLinkGenerator, config?.grails?.serverURL ?: 'http://localhost:8080')
            localeResolver(SessionLocaleResolver)
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                grailsApplication = grailsApplication
            }
            grailsDomainClassMappingContext(KeyValueMappingContext, 'test') {
                canInitializeEntities = true
            }
            jsonApiIdRenderStrategy(DefaultJsonApiIdRenderer)
            jsonViewConfiguration(JsonViewConfiguration)
            jsonTemplateEngine(JsonViewTemplateEngine, jsonViewConfiguration, grailsApplication.classLoader)
            jsonSmartViewResolver(JsonViewResolver, jsonTemplateEngine) {
                templateResolver = bean(PluginAwareTemplateResolver, jsonViewConfiguration)
            }
            jsonViewResolver(GenericGroovyTemplateViewResolver, jsonSmartViewResolver)
        }

    }
}
