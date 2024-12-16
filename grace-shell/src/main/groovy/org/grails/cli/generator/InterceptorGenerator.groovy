/*
 * Copyright 2022-2024 the original author or authors.
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
package org.grails.cli.generator

import groovy.transform.CompileStatic

import grails.cli.generator.AbstractGenerator
import grails.cli.generator.GenerationContext
import org.grails.build.parsing.CommandLine
import org.grails.config.CodeGenConfig

/**
 * @author Michael Yan
 * @since 2023.2.0
 */
@CompileStatic
class InterceptorGenerator extends AbstractGenerator {

    @Override
    boolean generate(GenerationContext generationContext) {
        CommandLine commandLine = generationContext.commandLine
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return
        }

        boolean overwrite = commandLine.hasOption('force') || commandLine.hasOption('f')
        CodeGenConfig config = loadApplicationConfig()
        String className = args[1].capitalize()
        String propertyName = className.uncapitalize()
        String defaultPackage = config.getProperty('grails.codegen.defaultPackage')
        String packagePath = defaultPackage.replace('.', '/')

        Map<String, Object> model = new HashMap<>()
        model['packageName'] = defaultPackage
        model['className'] = className
        model['propertyName'] = propertyName
        String interceptorClassFile = 'app/controllers/' + packagePath + '/' + className + 'Interceptor.groovy'
        String interceptorClassSpecFile = 'src/test/groovy/' + packagePath + '/' + className + 'InterceptorSpec.groovy'

        createFile('Interceptor.groovy.tpl', interceptorClassFile, model, overwrite)
        createFile('InterceptorSpec.groovy.tpl', interceptorClassSpecFile, model, overwrite)

        true
    }

    @Override
    boolean revoke(GenerationContext generationContext) {
        CommandLine commandLine = generationContext.commandLine
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return
        }

        CodeGenConfig config = loadApplicationConfig()
        String className = args[1].capitalize()
        String defaultPackage = config.getProperty('grails.codegen.defaultPackage')
        String packagePath = defaultPackage.replace('.', '/')

        String interceptorClassFile = 'app/controllers/' + packagePath + '/' + className + 'Interceptor.groovy'
        String interceptorClassSpecFile = 'src/test/groovy/' + packagePath + '/' + className + 'InterceptorSpec.groovy'

        removeFile(interceptorClassFile)
        removeFile(interceptorClassSpecFile)

        true
    }

}
