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
package org.grails.gradle.plugin.profiles.tasks

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.representer.Representer

import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.cli.profile.commands.script.GroovyScriptCommandTransform

/**
 * Compiles the classes for a profile
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
abstract class ProfileCompilerTask extends AbstractCompile {

    public static final String DEFAULT_COMPATIBILITY = '17'
    public static final String PROFILE_NAME = 'name'
    public static final String PROFILE_COMMANDS = 'commands'

    private final ObjectFactory objectFactory

    @Input
    abstract Property<String> getProjectName()

    @Optional
    @InputFile
    abstract RegularFileProperty getProfileConfig()

    @Optional
    @InputFiles
    abstract DirectoryProperty getFeaturesDir()

    @Optional
    @InputFiles
    abstract DirectoryProperty getTemplatesDir()

    @Input
    abstract Property<ResolvedComponentResult> getProfileDependencyRoot()

    @OutputFile
    abstract RegularFileProperty getProfileFile()

    @Override
    @InputFiles
    FileTree getSource() {
        (super.getSource() + this.objectFactory.fileTree().from(profileConfig)).asFileTree
    }

    @Inject
    ProfileCompilerTask(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        setSourceCompatibility(DEFAULT_COMPATIBILITY)
        setTargetCompatibility(DEFAULT_COMPATIBILITY)
        getProjectName().convention(project.provider(project::getName))
        getProfileConfig().set(getDestinationDirectory().file('META-INF/grails-profile/profile.yml'))
    }

    @TaskAction
    void execute() {
        boolean profileYmlExists = this.profileConfig.getAsFile().get().exists()

        DumperOptions options = new DumperOptions()
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(new DumperOptions()), options)
        Map<String, Object> profileData
        if (profileYmlExists) {
            profileData = (Map<String, Object>) this.profileConfig.getAsFile().get().withReader { BufferedReader r ->
                yaml.load(r)
            }
        }
        else {
            profileData = new LinkedHashMap<String, Object>()
        }

        profileData.put(PROFILE_NAME, projectName.get())

        this.profileFile.getAsFile().get().parentFile.mkdirs()

        if (!profileData.containsKey('extends')) {
            List<String> dependencies = []
            getProfileDependencyRoot().get().dependencies.each { DependencyResult result ->
                dependencies.add(result.requested.displayName)
            }
            profileData.put('extends', dependencies.join(','))
        }

        Set<File> groovySourceFiles = getSource().files.findAll { File f ->
            f.name.endsWith('.groovy')
        }
        Set<File> ymlSourceFiles = getSource().files.findAll { File f ->
            f.name.endsWith('.yml') && f.name != 'profile.yml'
        }

        LinkedHashMap<String, String> commandNames = new LinkedHashMap<>()
        for (File f in groovySourceFiles) {
            String fn = f.name
            commandNames.put(fn - '.groovy', fn)
        }
        for (File f in ymlSourceFiles) {
            String fn = f.name
            commandNames.put(fn - '.yml', fn)
        }

        if (commandNames) {
            profileData.put(PROFILE_COMMANDS, commandNames.sort { it.key })
        }

        if (profileYmlExists) {
            File parentDir = this.profileConfig.getAsFile().get().parentFile.canonicalFile
            File featuresDir = this.featuresDir.getOrNull()?.asFile ?: new File(parentDir, 'features')
            File[] featureDirs = featuresDir.listFiles({ File f ->
                f.isDirectory() && !f.name.startsWith('.') } as FileFilter)

            if (featureDirs) {
                Map map = (Map) profileData.get('features')
                if (map == null) {
                    map = [:]
                    profileData.put('features', map)
                }
                List<String> featureNames = new ArrayList<>()
                for (f in featureDirs) {
                    featureNames.add f.name
                }
                if (featureNames) {
                    map.put('provided', featureNames.sort { it })
                }
                profileData.put('features', map.sort { it.key })
            }
        }

        List<String> templates = []
        if (this.templatesDir.getAsFile().get().exists()) {
            this.objectFactory.fileTree().from(this.templatesDir).visit { FileVisitDetails f ->
                if (!f.isDirectory() && !f.name.startsWith('.')) {
                    templates.add f.relativePath.pathString
                }
            }
        }

        if (templates) {
            profileData.put('templates', templates.sort())
        }

        this.profileFile.getAsFile().get().withWriter { BufferedWriter w ->
            yaml.dump(profileData, w)
        }

        if (groovySourceFiles) {
            CompilerConfiguration configuration = new CompilerConfiguration()
            configuration.setScriptBaseClass(GroovyScriptCommand.name)
            this.destinationDirectory.getAsFile().getOrNull()?.mkdirs()
            configuration.setTargetDirectory(this.destinationDirectory.getAsFile().getOrNull())

            ImportCustomizer importCustomizer = new ImportCustomizer()
            importCustomizer.addStarImports('org.grails.cli.command.completers')
            importCustomizer.addStarImports('grails.util')
            importCustomizer.addStarImports('grails.codegen.model')
            configuration.addCompilationCustomizers(importCustomizer, new ASTTransformationCustomizer(new GroovyScriptCommandTransform()))

            for (File source in groovySourceFiles) {
                CompilationUnit compilationUnit = new CompilationUnit(configuration)
                configuration.compilationCustomizers.clear()
                configuration.compilationCustomizers.addAll(importCustomizer, new ASTTransformationCustomizer(new GroovyScriptCommandTransform()))
                compilationUnit.addSource(source)
                compilationUnit.compile()
            }
        }
    }

}
