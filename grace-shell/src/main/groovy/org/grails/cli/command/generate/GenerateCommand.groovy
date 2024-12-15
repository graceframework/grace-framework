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
package org.grails.cli.command.generate

import groovy.transform.CompileStatic

import grails.build.logging.GrailsConsole
import grails.cli.generator.GenerationContext
import grails.cli.generator.Generator
import org.grails.build.parsing.CommandLine
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProjectCommand

/**
 * Generate command uses Groovy templates to create everything you need.
 *
 * @author Michael Yan
 * @since 2023.2.0
 */
@CompileStatic
class GenerateCommand implements ProjectCommand {

    static final String USAGE = 'grace generate GENERATOR [args] [options]'
    static final String EXAMPLES = '''
    # Generate Grace artefacts
        $ grace generate domain Post title:string
        $ grace generate controller Post index create
'''

    final String name = 'generate'
    final CommandDescription description = new CommandDescription(name, 'Generate all for you need', USAGE, ['g'])

    GenerateCommand() {
        this.description.flag([name: 'help', aliases: '-h', type: 'boolean', description: "Print generator's options and usage", required: false])
                .flag([name: 'force', aliases: '-f', type: 'boolean', description: 'Overwrite files that already exist', required: false])
                .flag([name: 'skip', aliases: '-s', type: 'boolean', description: 'Skip files that already exist', required: false])
                .flag([name: 'quiet', aliases: '-q', type: 'boolean', description: 'Suppress status output', required: false])
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        CommandLine commandLine = executionContext.commandLine
        GenerationContext generationContext = new GenerationContext()
        generationContext.commandLine = commandLine

        if (commandLine.hasOption(CommandLine.HELP_ARGUMENT) || commandLine.hasOption('h')) {
            if (commandLine.remainingArgs) {
                String generatorName = commandLine.remainingArgs[0]
                Generator generator = loadedGenerators().find { it.name == generatorName }
                if (generator) {
                    return generator.help(generationContext)
                }
                else {
                    noGenerator(generatorName, executionContext.console)
                    printCommandHelp(executionContext.console)
                }
            }
            else {
                printCommandHelp(executionContext.console)
            }
            return true
        }

        if (!commandLine.remainingArgs) {
            printCommandHelp(executionContext.console)
            return false
        }

        String generatorName = commandLine.remainingArgs[0]
        Generator generator = loadedGenerators().find { it.name == generatorName }
        if (generator) {
            return generator.generate(generationContext)
        }
        else {
            noGenerator(generatorName, executionContext.console)
            printCommandHelp(executionContext.console)
        }

        true
    }

    void printCommandHelp(GrailsConsole console) {
        console.out.println('Usage:')
        console.out.println('  ' + USAGE)
        console.out.println()

        if (!this.description.getFlags().isEmpty()) {
            console.out.println('General options:')
        }
        this.description.getFlags().each {f ->
            def flag = new StringBuilder()
            flag << '  ' << (f.aliases ? "$f.aliases, " : '    ')
            if (f.type == 'boolean') {
                flag << "[--${f.name}]".padRight(30)
            }
            else {
                flag << "[--${f.name}=${f.banner}]".padRight(30)
            }
            flag << '# ' + f.description
            console.out.println(flag)
        }
        if (!this.description.getFlags().isEmpty()) {
            console.out.println()
        }

        console.out.println('Please choose a generator below:')
        console.out.println()
        loadedGenerators().each { Generator generator ->
            println '  ' + generator.name
        }
        console.out.println()
        console.out.println('Example:')
        console.out.println(EXAMPLES)
    }

    void noGenerator(String generatorName, GrailsConsole console) {
        console.error("Generator [$generatorName] not found, please choose the right generator!")
        console.out.println()
    }

    private static Collection<Generator> loadedGenerators() {
        List<Generator> allGenerators = new ArrayList<>()
        ServiceLoader.load(Generator).forEach { Generator generator ->
            allGenerators.add(generator)
        }
        allGenerators.sort(true) {
            it.name
        }
    }

}
