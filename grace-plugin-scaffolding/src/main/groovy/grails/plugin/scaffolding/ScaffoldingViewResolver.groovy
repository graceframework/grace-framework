/*
 * Copyright 2015-2025 the original author or authors.
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
package grails.plugin.scaffolding

import java.util.concurrent.ConcurrentHashMap

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.transform.CompileStatic
import org.springframework.context.ResourceLoaderAware
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.UrlResource
import org.springframework.web.servlet.View

import grails.codegen.model.Model
import grails.codegen.model.ModelBuilder
import grails.core.GrailsControllerClass
import grails.io.IOUtils
import grails.util.BuildSettings
import grails.util.Environment

import org.grails.buffer.FastStringWriter
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.servlet.view.GroovyPageView
import org.grails.web.servlet.view.GroovyPageViewResolver

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
class ScaffoldingViewResolver extends GroovyPageViewResolver implements ResourceLoaderAware, ModelBuilder {

    ResourceLoader resourceLoader
    protected Map<String, View> generatedViewCache = new ConcurrentHashMap<>()

    protected String buildCacheKey(String viewName) {
        String viewCacheKey = groovyPageLocator.resolveViewFormat(viewName)
        String currentControllerKeyPrefix = resolveCurrentControllerKeyPrefixes(viewName.startsWith('/'))
        if (currentControllerKeyPrefix != null) {
            viewCacheKey = currentControllerKeyPrefix + ':' + viewCacheKey
        }
        viewCacheKey
    }

    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        View view = super.loadView(viewName, locale)

        if (view == null) {
            String cacheKey = buildCacheKey(viewName)
            view = generatedViewCache.get(cacheKey)
            if (view != null) {
                return view
            }

            GrailsWebRequest webR = GrailsWebRequest.lookup()
            GrailsControllerClass controllerClass = webR.controllerClass

            def scaffoldValue = controllerClass?.getPropertyValue('scaffold')
            if (scaffoldValue instanceof Class) {
                String shortViewName = viewName.substring(viewName.lastIndexOf('/') + 1)
                Resource res = null

                if (Environment.isDevelopmentMode()) {
                    res = new FileSystemResource(new File(BuildSettings.BASE_DIR, "src/main/templates/scaffolding/${shortViewName}.gsp"))
                }

                if (!res?.exists()) {
                    URL url = IOUtils.findResourceRelativeToClass(controllerClass.clazz, "/templates/scaffolding/${shortViewName}.gsp")
                    res = url ? new UrlResource(url) : null
                }

                if (!res.exists()) {
                    res = resourceLoader.getResource("classpath:META-INF/templates/scaffolding/${shortViewName}.gsp")
                }

                if (res.exists()) {
                    Model model = model((Class) scaffoldValue)
                    def viewGenerator = new GStringTemplateEngine()
                    Template t = viewGenerator.createTemplate(res.URL)

                    FastStringWriter contents = new FastStringWriter()
                    t.make(model.asMap()).writeTo(contents)

                    Resource resource = new ByteArrayResource(contents.toString().getBytes(templateEngine.gspEncoding), "view:$cacheKey")
                    Template template = templateEngine.createTemplate(resource)
                    view = new GroovyPageView()
                    view.setServletContext(getServletContext())
                    view.setTemplate(template)
                    view.setApplicationContext(getApplicationContext())
                    view.setTemplateEngine(templateEngine)
                    view.afterPropertiesSet()
                    generatedViewCache.put(cacheKey, view)
                    return view
                }

                return view
            }
        }
        return view
    }

}
