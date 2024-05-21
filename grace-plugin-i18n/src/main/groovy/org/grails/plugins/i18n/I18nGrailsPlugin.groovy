/*
 * Copyright 2004-2024 the original author or authors.
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
package org.grails.plugins.i18n

import java.nio.file.Files

import groovy.util.logging.Slf4j
import org.springframework.core.PriorityOrdered
import org.springframework.core.io.Resource

import grails.plugins.Plugin
import grails.util.BuildSettings
import grails.util.GrailsUtil

import org.grails.spring.context.support.ReloadableResourceBundleMessageSource

/**
 * Configures Grails' internationalisation support.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.4
 */
@Slf4j
class I18nGrailsPlugin extends Plugin implements PriorityOrdered {

    String version = GrailsUtil.getGrailsVersion()
    def watchedResources = ['file:./grails-app/i18n/**/*.properties',
                            'file:./app/i18n/**/*.properties']

    @Override
    Closure doWithSpring() {
        { ->
        }
    }

    @Override
    void doWithDynamicMethods() {
        String.metaClass.getMessage = { ->
            def messageSource = applicationContext.messageSource
            messageSource.getMessage(delegate, null, Locale.ENGLISH)
        }
        String.metaClass.message = { String lang ->
            def locale = Locale.forLanguageTag(lang.replace('_', '-'))
            def messageSource = applicationContext.messageSource
            messageSource.getMessage(delegate, null, locale)
        }
        String.metaClass.message = { List<Object> args, String lang ->
            def locale = Locale.forLanguageTag(lang.replace('_', '-'))
            def messageSource = applicationContext.messageSource
            messageSource.getMessage(delegate, args.toArray(), locale)
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        def ctx = applicationContext
        def application = grailsApplication
        if (!ctx) {
            log.debug("Application context not found. Can't reload")
            return
        }

        boolean nativeascii = application.config.getProperty('grails.enable.native2ascii', Boolean, true)
        def resourcesDir = BuildSettings.RESOURCES_DIR
        def classesDir = BuildSettings.CLASSES_DIR
        def i18nDir = new File(BuildSettings.GRAILS_APP_DIR, 'i18n')

        if (resourcesDir.exists() && event.source instanceof Resource) {
            File eventFile = event.source.file.canonicalFile
            if (isChildOfFile(eventFile, i18nDir)) {
                if (nativeascii) {
                    // if native2ascii is enabled then read the properties and write them out again
                    // so that unicode escaping is applied
                    def properties = new Properties()
                    eventFile.withReader {
                        properties.load(it)
                    }
                    // by using an OutputStream the unicode characters will be escaped
                    new File(resourcesDir, eventFile.name).withOutputStream {
                        properties.store(it, '')
                    }
                    new File(classesDir, eventFile.name).withOutputStream {
                        properties.store(it, '')
                    }
                }
                else {
                    // otherwise just copy the file as is
                    Files.copy(eventFile.toPath(), new File(resourcesDir, eventFile.name).toPath())
                    Files.copy(eventFile.toPath(), new File(classesDir, eventFile.name).toPath())
                }
            }
        }

        def messageSource = ctx.getBean('messageSource')
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
    }

    protected boolean isChildOfFile(File child, File parent) {
        def currentFile = child.canonicalFile
        while (currentFile != null) {
            if (currentFile == parent) {
                return true
            }
            currentFile = currentFile.parentFile
        }
        false
    }

    @Override
    int getOrder() {
        20
    }

}
