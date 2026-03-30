/*
 * Copyright 2015-2026 the original author or authors.
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

import org.grails.cli.command.completers.DomainClassCompleter

description('Generates an asynchronous Controller that performs CRUD operations') {
    usage 'grace generate-async-controller [DOMAIN CLASS]'
    completer DomainClassCompleter
    flag name: 'force', description: 'Whether to overwrite existing files'
}

if (args) {
    def classNames = args
    if (args[0] == '*') {
        classNames = resources('file:app/domain/**/*.groovy')
                    .collect { className(it) }
    }

    for (arg in classNames) {
        def sourceClass = source(arg)
        def overwrite = flag('force') ? true : false

        if (sourceClass) {
            def model = model(sourceClass)

            render template: template('scaffolding/AsyncController.groovy'), 
                    destination: file("app/controllers/${model.packagePath}/${model.convention('Controller')}.groovy"),
                    model: model,
                    overwrite: overwrite

            render template: template('scaffolding/AsyncSpec.groovy'), 
                    destination: file("src/test/groovy/${model.packagePath}/${model.convention('ControllerSpec')}.groovy"),
                    model: model,
                    overwrite: overwrite

            addStatus "Scaffolding completed for ${projectPath(sourceClass)}"                                         
        }
        else {
            error "Domain class not found for name $arg"
        }
    }
}
else {
    error 'No domain class specified'
}
