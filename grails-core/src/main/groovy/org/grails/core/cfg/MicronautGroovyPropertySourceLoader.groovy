/*
 * Copyright 2020-2022 the original author or authors.
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
package org.grails.core.cfg

import java.util.stream.Stream

import groovy.transform.CompileStatic
import io.micronaut.context.env.AbstractPropertySourceLoader
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.io.ResourceLoader

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata

import org.grails.config.NavigableMap

/**
 * Loads properties from a Groovy script.
 *
 * @author Puneet Behl
 * @since 4.0.3
 */
@CompileStatic
class MicronautGroovyPropertySourceLoader extends AbstractPropertySourceLoader {

    @Override
    int getOrder() {
        DEFAULT_POSITION
    }

    @Override
    protected void processInput(String name, InputStream input, Map<String, Object> finalMap) throws IOException {
        def env = Environment.current.name
        if (input.available()) {
            ConfigSlurper configSlurper = env ? new ConfigSlurper(env) : new ConfigSlurper()
            configSlurper.setBinding(
                    userHome: System.getProperty('user.home'),
                    grailsHome: BuildSettings.GRAILS_HOME?.absolutePath,
                    appName: Metadata.getCurrent().getApplicationName(),
                    appVersion: Metadata.getCurrent().getApplicationVersion())
            try {
                def configObject = configSlurper.parse(input.getText('UTF-8'))
                def propertySource = new NavigableMap()
                propertySource.merge(configObject.flatten(), false)
                finalMap.putAll(propertySource)
            }
            catch (Throwable e) {
                throw new ConfigurationException("Exception occurred reading configuration [${name}]: ${e.getMessage()}", e)
            }
        }
    }

    @Override
    protected Optional<InputStream> readInput(ResourceLoader resourceLoader, String fileName) {
        Stream<URL> urls = resourceLoader.getResources(fileName)
        Stream<URL> urlStream = urls.filter({ url -> !url.getPath().contains('src/main/groovy') })
        Optional<URL> config = urlStream.findFirst()
        if (config.isPresent()) {
            return config.flatMap({ url ->
                try {
                    return Optional.of(url.openStream())
                }
                catch (IOException e) {
                    throw new ConfigurationException("Exception occurred reading configuration [${fileName}]: ${e.getMessage()}", e)
                }
            })
        }
        Optional.empty()
    }

    @Override
    Set<String> getExtensions() {
        Collections.singleton('groovy')
    }

}
