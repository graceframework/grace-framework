/*
 * Copyright 2022-2026 the original author or authors.
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
import grails.util.GrailsNameUtils

/**
 * @author Michael Yan
 * @since 2024.1.0
 */
@CompileStatic
class ScriptGenerator extends AbstractGenerator {

    @Override
    boolean generate() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return false
        }

        boolean overwrite = commandLine.hasOption('force') || commandLine.hasOption('f')
        String scriptName = GrailsNameUtils.getNameFromScript(args[1])
        if (scriptName.endsWith('.groovy')) {
            scriptName = scriptName - '.groovy'
        }

        Map<String, Object> model = new HashMap<>()
        model['scriptName'] = GrailsNameUtils.getScriptName(scriptName)

        String scriptFile = 'src/main/scripts/' + scriptName + '.groovy'
        createFile('Script.groovy.tpl', scriptFile, model, overwrite)

        true
    }

    @Override
    boolean revoke() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return false
        }

        String scriptName = GrailsNameUtils.getNameFromScript(args[1])
        if (scriptName.endsWith('.groovy')) {
            scriptName = scriptName - '.groovy'
        }
        String scriptFile = 'src/main/scripts/' + scriptName + '.groovy'

        removeFile(scriptFile)

        true
    }

}
