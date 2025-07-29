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

/**
 * @author Michael Yan
 * @since 2023.2.0
 */
@CompileStatic
class ControllerGenerator extends AbstractGenerator {

    @Override
    boolean generate() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return
        }

        boolean overwrite = commandLine.hasOption('force') || commandLine.hasOption('f')
        String className = args[1].capitalize()
        String propertyName = className.uncapitalize()
        String[] actionNames = args.size() >= 3 ? args[2..-1] : ['index']

        Map<String, Object> model = new HashMap<>()
        model['packageName'] = defaultPackageName
        model['className'] = className
        model['propertyName'] = propertyName
        model['actions'] = actionNames

        String controllerFile = 'app/controllers/' + defaultPackagePath + '/' + className + 'Controller.groovy'
        String controllerSpecFile = 'src/test/groovy/' + defaultPackagePath + '/' + className + 'ControllerSpec.groovy'
        createFile('Controller.groovy.tpl', controllerFile, model, overwrite)
        createFile('ControllerSpec.groovy.tpl', controllerSpecFile, model, overwrite)

        actionNames.each { String actionName ->
            String gspViewFile = 'app/views/' + propertyName + '/' + actionName + '.gsp'
            model['action'] = actionName
            model['path'] = gspViewFile
            createFile('view.gsp.tpl', gspViewFile, model, overwrite)
        }

        true
    }

    @Override
    boolean revoke() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return
        }

        String className = args[1].capitalize()
        String propertyName = className.uncapitalize()

        String controllerFile = 'app/controllers/' + defaultPackagePath + '/' + className + 'Controller.groovy'
        String controllerSpecFile = 'src/test/groovy/' + defaultPackagePath + '/' + className + 'ControllerSpec.groovy'
        removeFile(controllerFile)
        removeFile(controllerSpecFile)

        String gspViewDir = 'app/views/' + propertyName
        new File(gspViewDir).list().each {
            removeFile(gspViewDir + '/' + it)
        }

        true
    }

}
