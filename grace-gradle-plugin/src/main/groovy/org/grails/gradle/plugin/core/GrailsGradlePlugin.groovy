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
package org.grails.gradle.plugin.core

import javax.inject.Inject

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.apache.tools.ant.filters.EscapeUnicode
import org.apache.tools.ant.filters.ReplaceTokens
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.JavaForkOptions
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun

import grails.dev.commands.ApplicationCommand
import grails.util.Environment
import grails.util.GrailsNameUtils
import grails.util.Metadata

import org.grails.build.parsing.CommandLineParser
import org.grails.cli.compiler.dependencies.GrailsDependenciesDependencyManagement
import org.grails.core.io.support.GrailsFactoriesLoader
import org.grails.gradle.plugin.commands.ApplicationContextCommandTask
import org.grails.gradle.plugin.commands.ApplicationContextScriptTask
import org.grails.gradle.plugin.model.GrailsClasspathToolingModelBuilder
import org.grails.gradle.plugin.run.FindMainClassTask
import org.grails.gradle.plugin.util.BuildSettings
import org.grails.gradle.plugin.util.SourceSets

/**
 * The main Grails gradle plugin implementation
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GrailsGradlePlugin extends GroovyPlugin {

    public static final String CONSOLE_CONFIGURATION = 'console'
    public static final String PROFILE_CONFIGURATION = 'profile'

    List<Class<Plugin>> basePluginClasses = [IntegrationTestGradlePlugin] as List<Class<Plugin>>
    List<String> excludedGrailsAppSourceDirs = ['assets', 'scripts']
    List<String> grailsAppResourceDirs = ['i18n', 'conf']
    private final ToolingModelBuilderRegistry registry
    String grailsVersion

    @Inject
    GrailsGradlePlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry
    }

    void apply(Project project) {
        verifyGradleVersion()

        registerGrailsExtension(project)

        grailsVersion = resolveGrailsVersion(project)

        // Keep configure system properties First
        configureGrailsBuildSettings(project)

        // reset the environment to ensure it is resolved again for each invocation
        Environment.reset()

        if (project.tasks.findByName('compileGroovy') == null) {
            super.apply(project)
        }

        configureProfile(project)

        applyDefaultPlugins(project)

        registerToolingModelBuilder(project, registry)

        applyBasePlugins(project)

        registerFindMainClassTask(project)

        enableNative2Ascii(project, grailsVersion)

        configureSpringBootExtension(project)

        configureForkSettings(project, grailsVersion)

        configureGrailsSourceDirs(project)

        createBuildPropertiesTask(project)

        configureGroovyASTMetadata(project)
    }

    protected void configureProfile(Project project) {
        if (project.configurations.findByName(PROFILE_CONFIGURATION) == null) {
            def profileConfiguration = project.configurations.create(PROFILE_CONFIGURATION)
            profileConfiguration.incoming.beforeResolve {
                if (!profileConfiguration.allDependencies) {
                    addDefaultProfile(project, profileConfiguration)
                }
            }
        }
    }

    protected void applyDefaultPlugins(Project project) {
        applySpringBootPlugin(project)

        Plugin dependencyManagementPlugin = project.plugins.findPlugin(DependencyManagementPlugin)
        if (dependencyManagementPlugin == null) {
            project.plugins.apply(DependencyManagementPlugin)
        }

        DependencyManagementExtension dme = project.extensions.findByType(DependencyManagementExtension)

        applyBomImport(dme, project)
    }

    protected void applySpringBootPlugin(Project project) {
        def springBoot = project.extensions.findByType(SpringBootExtension)
        if (!springBoot) {
            project.plugins.apply(SpringBootPlugin)
        }
    }

    @CompileDynamic
    private void applyBomImport(DependencyManagementExtension dme, Project project) {
        String grailsVersion = resolveGrailsVersion(project)
        dme.imports({
            mavenBom("org.graceframework:grace-bom:${grailsVersion}")
        })
        dme.setApplyMavenExclusions(false)
        dme.generatedPomCustomization {
            enabled = false
        }
    }

    protected String getDefaultProfile() {
        'web'
    }

    void addDefaultProfile(Project project, Configuration profileConfig) {
        project.dependencies.add(PROFILE_CONFIGURATION, "org.graceframework.profiles:${System.getProperty('grails.profile') ?: defaultProfile}:")
    }

    @CompileDynamic
    protected Task createBuildPropertiesTask(Project project) {
        if (project.tasks.findByName('buildProperties') == null) {
            File resourcesDir = SourceSets.findMainSourceSet(project).output.resourcesDir
            File buildInfoFile = new File(resourcesDir, 'META-INF/grails.build.info')

            Task buildPropertiesTask = project.tasks.create('buildProperties')
            Map<String, Object> buildPropertiesContents = [
                    'grails.env': Environment.isSystemSet() ? Environment.current.getName() : Environment.PRODUCTION.getName(),
                    'info.app.name': project.name,
                    'info.app.version': project.version instanceof Serializable ? project.version : project.version.toString(),
                    'info.app.grailsVersion': grailsVersion]

            buildPropertiesTask.group = 'build'
            buildPropertiesTask.description = "Build properties into 'META-INF/grails.build.info'."
            buildPropertiesTask.inputs.properties(buildPropertiesContents)
            buildPropertiesTask.outputs.file(buildInfoFile)
            buildPropertiesTask.doLast {
                project.buildDir.mkdirs()
                ant.mkdir(dir: buildInfoFile.parentFile)
                ant.propertyfile(file: buildInfoFile) {
                    for (me in buildPropertiesTask.inputs.properties) {
                        entry key: me.key, value: me.value
                    }
                }
            }

            project.afterEvaluate {
                TaskContainer tasks = project.tasks
                tasks.findByName('processResources')?.dependsOn(buildPropertiesTask)
            }
        }
    }

    @CompileStatic
    protected void configureSpringBootExtension(Project project) {
        project.getTasks().withType(BootRun).configureEach { BootRun bootRun ->
            bootRun.doFirst("Configure System Properties") {
                String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
                bootRun.systemProperty(BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
                bootRun.systemProperty(BuildSettings.APP_DIR, grailsAppPath ? project.file(grailsAppPath).absolutePath : '')
                bootRun.systemProperty(BuildSettings.PROJECT_TARGET_DIR, project.buildDir.absolutePath)
                bootRun.systemProperty(BuildSettings.PROJECT_RESOURCES_DIR, new File(project.buildDir, 'resources/main').absolutePath)
                bootRun.systemProperty(BuildSettings.PROJECT_CLASSES_DIR, new File(project.buildDir, 'classes/groovy/main').absolutePath)
            }
        }
    }

    @CompileStatic
    protected void registerToolingModelBuilder(Project project, ToolingModelBuilderRegistry registry) {
        registry.register(new GrailsClasspathToolingModelBuilder())
    }

    @CompileStatic
    protected void applyBasePlugins(Project project) {
        for (Class<Plugin> cls in basePluginClasses) {
            project.plugins.apply(cls)
        }
    }

    protected GrailsExtension registerGrailsExtension(Project project) {
        if (project.extensions.findByName('grails') == null) {
            project.extensions.create(GrailsExtension, 'grails', GrailsExtension, project)
        }
    }

    @CompileStatic
    protected String configureGrailsBuildSettings(Project project) {
        project.afterEvaluate {
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            System.setProperty(BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
            System.setProperty(BuildSettings.APP_DIR, grailsAppPath ? project.file(grailsAppPath).absolutePath : '')
            System.setProperty(BuildSettings.PROJECT_TARGET_DIR, project.buildDir.absolutePath)
            System.setProperty(BuildSettings.PROJECT_RESOURCES_DIR, new File(project.buildDir, 'resources/main').absolutePath)
            System.setProperty(BuildSettings.PROJECT_CLASSES_DIR, new File(project.buildDir, 'classes/groovy/main').absolutePath)
        }
    }

    protected void configureApplicationCommands(Project project) {
        Configuration runtimeClasspath = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration consoleClasspath = project.configurations.getByName(CONSOLE_CONFIGURATION)
        Configuration profileClasspath = project.configurations.getByName(PROFILE_CONFIGURATION)

        URL[] urls = [project.layout.buildDirectory.dir('classes/groovy/main').get().asFile.toURI().toURL()]
        ClassLoader classLoader = new URLClassLoader(urls, GrailsFactoriesLoader.classLoader)
        List<ApplicationCommand> applicationContextCommands = GrailsFactoriesLoader.loadFactories(ApplicationCommand, classLoader)

        for (ApplicationCommand ctxCommand in applicationContextCommands) {
            String taskName = GrailsNameUtils.getLogicalPropertyName(ctxCommand.class.name, 'Command')
            String commandName = GrailsNameUtils.getScriptName(GrailsNameUtils.getLogicalName(ctxCommand.class.name, 'Command'))
            String commandDescription = ctxCommand.description
            project.tasks.register(taskName, ApplicationContextCommandTask) { ApplicationContextCommandTask commandTask ->
                commandTask.setGroup("Command")
                commandTask.setDescription(commandDescription)
                commandTask.classpath = buildClasspath(project, runtimeClasspath, consoleClasspath, profileClasspath)
                commandTask.command = commandName
                commandTask.systemProperty 'spring.main.banner-mode', 'OFF'
                commandTask.systemProperty 'logging.level.ROOT', 'OFF'
                commandTask.systemProperty 'spring.output.ansi.enabled', 'always'
                commandTask.systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
                if (project.hasProperty('args')) {
                    commandTask.args(CommandLineParser.translateCommandline(project.getProperties().get('args') as String))
                }
            }
        }
    }

    @CompileStatic
    protected void configureGrailsSourceDirs(Project project) {
        project.afterEvaluate {
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            if (grailsAppPath) {
                SourceSet mainSourceSet = project.getExtensions().getByType(JavaPluginExtension).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                mainSourceSet.getExtensions().getByType(GroovySourceDirectorySet).setSrcDirs(resolveGrailsSourceDirs(project, grailsAppPath))
                mainSourceSet.getResources().setSrcDirs(resolveGrailsResourceDirs(project, grailsAppPath))
            }
        }
    }

    @CompileStatic
    protected List<File> resolveGrailsResourceDirs(Project project, String grailsAppPath) {
        List<File> grailsResourceDirs = [project.file('src/main/resources')]
        for (String f in grailsAppResourceDirs) {
            grailsResourceDirs.add(project.file("${grailsAppPath}/${f}"))
        }
        grailsResourceDirs
    }

    @CompileStatic
    protected List<File> resolveGrailsSourceDirs(Project project, String grailsAppPath) {
        List<File> grailsSourceDirs = []
        project.file(grailsAppPath).eachDir { File subdir ->
            if (isGrailsSourceDirectory(subdir)) {
                grailsSourceDirs.add(subdir)
            }
        }
        grailsSourceDirs.add(project.file('src/main/groovy'))
        grailsSourceDirs
    }

    @CompileStatic
    protected boolean isGrailsSourceDirectory(File subdir) {
        def dirName = subdir.name
        !subdir.hidden && !dirName.startsWith('.') && !excludedGrailsAppSourceDirs.contains(dirName) && !grailsAppResourceDirs.contains(dirName)
    }

    protected String resolveGrailsVersion(Project project) {
        def grailsVersion = project.findProperty('graceVersion') ?: project.findProperty('grailsVersion')

        grailsVersion = grailsVersion ?: new GrailsDependenciesDependencyManagement().getGrailsVersion()

        grailsVersion
    }

    protected void configureForkSettings(Project project, String grailsVersion) {
        def systemPropertyConfigurer = { String defaultGrailsEnv, JavaForkOptions task ->
            def map = System.properties.findAll { entry ->
                entry.key?.toString()?.startsWith('grails.')
            }
            for (key in map.keySet()) {
                def value = map.get(key)
                if (value) {
                    def sysPropName = key.toString().substring(7)
                    task.systemProperty(sysPropName, value.toString())
                }
            }
            task.systemProperty Metadata.APPLICATION_NAME, project.name
            task.systemProperty Metadata.APPLICATION_VERSION, project.version
            task.systemProperty Metadata.APPLICATION_GRAILS_VERSION, grailsVersion
            task.systemProperty Environment.KEY, defaultGrailsEnv
            task.systemProperty Environment.FULL_STACKTRACE, System.getProperty(Environment.FULL_STACKTRACE) ?: ''
            if (task.minHeapSize == null) {
                task.minHeapSize = '768m'
            }
            if (task.maxHeapSize == null) {
                task.maxHeapSize = '768m'
            }
            List<String> jvmArgs = task.jvmArgs

            task.jvmArgs '-XX:+TieredCompilation', '-XX:TieredStopAtLevel=1', '-XX:CICompilerCount=3'

            // Copy GRAILS_FORK_OPTS into the fork. Or use GRAILS_OPTS if no fork options provided
            // This allows run-app etc. to run using appropriate settings and allows users to provided
            // different FORK JVM options to the build options.
            def envMap = System.getenv()
            String opts = envMap.GRAILS_FORK_OPTS ?: envMap.GRAILS_OPTS
            if (opts) {
                task.jvmArgs opts.split(' ')
            }
        }

        TaskContainer tasks = project.tasks

        String grailsEnvSystemProperty = System.getProperty(Environment.KEY)
        tasks.withType(Test).each systemPropertyConfigurer.curry(grailsEnvSystemProperty ?: Environment.TEST.getName())
        tasks.withType(JavaExec).each systemPropertyConfigurer.curry(grailsEnvSystemProperty ?: Environment.DEVELOPMENT.getName())
    }

    protected void configureConsoleTask(Project project) {
        TaskContainer tasks = project.tasks
        if (project.configurations.findByName('console') == null) {
            def consoleConfiguration = project.configurations.create('console')
            def findMainClass = tasks.findByName('findMainClass')
            def consoleTask = createConsoleTask(project, tasks, consoleConfiguration)
            def shellTask = createShellTask(project, tasks, consoleConfiguration)

            findMainClass.doLast {
                ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) project.getExtensions().getByName('ext')
                def mainClassName = extraProperties.get('mainClassName')
                if (mainClassName) {
                    consoleTask.args mainClassName
                    shellTask.args mainClassName
                    project.tasks.withType(ApplicationContextCommandTask) { ApplicationContextCommandTask task ->
                        task.args mainClassName
                    }
                }
                project.tasks.withType(ApplicationContextScriptTask) { ApplicationContextScriptTask task ->
                    task.args mainClassName
                }
            }

            consoleTask.dependsOn(tasks.findByName('classes'), findMainClass)
            shellTask.dependsOn(tasks.findByName('classes'), findMainClass)
        }
    }

    @CompileDynamic
    protected JavaExec createConsoleTask(Project project, TaskContainer tasks, Configuration configuration) {
        tasks.create('console', JavaExec) {
            group = 'Grace'
            description = 'Runs the interactive Groovy Console.'
            systemProperty BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath
            systemProperty 'spring.devtools.restart.enabled', false
            systemProperty 'spring.output.ansi.enabled', 'always'
            classpath = project.sourceSets.main.runtimeClasspath + configuration
            mainClass.set('grails.ui.console.GrailsConsole')
        }
    }

    @CompileDynamic
    protected JavaExec createShellTask(Project project, TaskContainer tasks, Configuration configuration) {
        tasks.create('shell', JavaExec) {
            group = 'Grace'
            description = 'Runs the interactive Groovy Shell.'
            systemProperty BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath
            systemProperty 'spring.devtools.restart.enabled', false
            systemProperty 'spring.output.ansi.enabled', 'always'
            classpath = project.sourceSets.main.runtimeClasspath + configuration
            mainClass.set('grails.ui.shell.GrailsShell')
            standardInput = System.in
        }
    }

    protected void registerFindMainClassTask(Project project) {
        Task findMainClassTask = project.tasks.findByName('findMainClass')
        if (findMainClassTask == null) {
            project.tasks.register('findMainClass', FindMainClassTask).configure { FindMainClassTask task ->
                task.group = 'build'
                task.description = 'Finds the main class of the application.'
                task.mustRunAfter(project.tasks.named('classes'))
            }
        }
        else if (!FindMainClassTask.isAssignableFrom(findMainClassTask.class)) {
            project.tasks.register('grailsFindMainClass', FindMainClassTask).configure { FindMainClassTask task ->
                task.group = 'build'
                task.description = 'Finds the main class of the application.'
                task.mustRunAfter(project.tasks.named('classes'))
                task.dependsOn(findMainClassTask)
                findMainClassTask.finalizedBy(task)
            }
        }
    }

    /**
     * Enables native2ascii processing of resource bundles
     **/
    @CompileDynamic
    protected void enableNative2Ascii(Project project, String grailsVersion) {
        project.afterEvaluate {
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            TaskContainer taskContainer = project.tasks

            SourceSet sourceSet = SourceSets.findMainSourceSet(project)
            taskContainer.getByName(sourceSet.processResourcesTaskName) { AbstractCopyTask task ->
                GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)
                boolean native2ascii = grailsExt.isNative2ascii()
                task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
                if (native2ascii && grailsExt.isNative2asciiAnt() && !taskContainer.findByName('native2ascii')) {
                    File destinationDir = ((ProcessResources) task).destinationDir
                    Task native2asciiTask = createNative2AsciiTask(taskContainer, project.file("${grailsAppPath}/i18n"), destinationDir)
                    task.dependsOn(native2asciiTask)
                }

                Map<String, String> replaceTokens = [
                        'info.app.name': project.name,
                        'info.app.version': project.version?.toString(),
                        'info.app.grailsVersion': grailsVersion
                ]

                task.from(project.relativePath('src/main/templates')) {
                    into('templates')
                    include '**/*.gsp'
                }

                if (!native2ascii) {
                    task.from(sourceSet.resources) {
                        include '**/*.properties'
                        filter(ReplaceTokens, tokens: replaceTokens)
                    }
                }
                else if (!grailsExt.isNative2asciiAnt()) {
                    task.from(sourceSet.resources) {
                        include '**/*.properties'
                        filter(ReplaceTokens, tokens: replaceTokens)
                        filter(EscapeUnicode)
                    }
                }

                task.from(sourceSet.resources) {
                    filter(ReplaceTokens, tokens: replaceTokens)
                    include '**/*.groovy'
                    include '**/*.yml'
                    include '**/*.xml'
                }

                task.from(sourceSet.resources) {
                    exclude '**/*.properties'
                    exclude '**/*.groovy'
                    exclude '**/*.yml'
                    exclude '**/*.xml'
                }
            }
        }
    }

    @CompileDynamic
    protected Task createNative2AsciiTask(TaskContainer taskContainer, src, dest) {
        Task native2asciiTask = taskContainer.create('native2ascii')
        native2asciiTask.doLast {
            ant.native2ascii(src: src, dest: dest,
                    includes: '**/*.properties', encoding: 'UTF-8')
        }
        native2asciiTask.inputs.dir(src)
        native2asciiTask.outputs.dir(dest)
        native2asciiTask
    }

    @CompileDynamic
    protected Jar createPathingJarTask(Project project, String name, Configuration... configurations) {
        project.tasks.create(name, Jar) { Jar task ->
            task.dependsOn(configurations)
            task.archiveAppendix.set('pathing')
            task.setGroup('build')
            task.setDescription('Generates a pathing jar file.')

            Set files = []
            configurations.each {
                files.addAll(it.files)
            }

            task.doFirst {
                manifest { Manifest manifest ->
                    manifest.attributes 'Class-Path': files.collect { File file ->
                        file.toURI().toURL().toString().replaceFirst(/file:\/+/, '/')
                    }.join(' ')
                }
            }
        }
    }

    protected void configureRunScript(Project project) {
        Configuration runtimeClasspath = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration consoleClasspath = project.configurations.getByName(CONSOLE_CONFIGURATION)
        Configuration profileClasspath = project.configurations.getByName(PROFILE_CONFIGURATION)

        if (project.tasks.findByName('runScript') == null) {
            project.tasks.register('runScript', ApplicationContextScriptTask) { ApplicationContextScriptTask scriptTask ->
                scriptTask.group = 'Grace'
                scriptTask.description = "Executes the Grace Application Scripts."
                scriptTask.classpath = buildClasspath(project, runtimeClasspath, consoleClasspath, profileClasspath)
                scriptTask.systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
                scriptTask.systemProperty BuildSettings.APP_BASE_DIR, project.projectDir
                scriptTask.systemProperty 'spring.main.banner-mode', 'OFF'
                scriptTask.systemProperty 'logging.level.ROOT', 'OFF'
                scriptTask.systemProperty 'spring.devtools.restart.enabled', false
                scriptTask.systemProperty 'spring.output.ansi.enabled', 'always'
                if (project.hasProperty('args')) {
                    scriptTask.args(CommandLineParser.translateCommandline(project.getProperties().get('args') as String))
                }
            }
        }
    }

    protected void configureRunCommand(Project project) {
        Configuration runtimeClasspath = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration consoleClasspath = project.configurations.getByName(CONSOLE_CONFIGURATION)
        Configuration profileClasspath = project.configurations.getByName(PROFILE_CONFIGURATION)

        if (project.tasks.findByName('runCommand') == null) {
            project.tasks.register('runCommand', ApplicationContextCommandTask) { ApplicationContextCommandTask commandTask ->
                commandTask.group = 'Grace'
                commandTask.description = "Executes the Grace Application Commands."
                commandTask.classpath = buildClasspath(project, runtimeClasspath, consoleClasspath, profileClasspath)
                commandTask.systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
                commandTask.systemProperty BuildSettings.APP_BASE_DIR, project.projectDir
                commandTask.systemProperty 'spring.main.banner-mode', 'OFF'
                commandTask.systemProperty 'logging.level.ROOT', 'OFF'
                commandTask.systemProperty "spring.devtools.restart.enabled", false
                commandTask.systemProperty 'spring.output.ansi.enabled', 'always'
                if (project.hasProperty('args')) {
                    commandTask.args(CommandLineParser.translateCommandline(project.getProperties().get('args') as String))
                }
            }

            project.tasks.named('findMainClass').configure { Task findMainClass ->
                findMainClass.doLast {
                    ExtraPropertiesExtension extraProperties = project.getExtensions().getByType(ExtraPropertiesExtension)
                    def mainClassName = extraProperties.get('mainClassName')
                    if (mainClassName) {
                        project.tasks.withType(ApplicationContextCommandTask).configureEach { ApplicationContextCommandTask commandTask ->
                            commandTask.args mainClassName
                        }
                    }
                }
            }
        }
    }

    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(new File(project.buildDir, 'classes/groovy/main'))
    }

    @CompileDynamic
    protected void configurePathingJar(Project project) {
        project.afterEvaluate {
            if (project.tasks.findByName('pathingJar') == null) {
                ConfigurationContainer configurations = project.configurations
                Configuration runtime = configurations.getByName('runtimeClasspath')
                Configuration developmentOnly = configurations.findByName('developmentOnly')
                Configuration console = configurations.getByName('console')
                SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)
                SourceSetOutput output = mainSourceSet?.output
                FileCollection mainFiles = resolveClassesDirs(output, project)

                Jar pathingJar

                if (developmentOnly != null) {
                    pathingJar = createPathingJarTask(project, 'pathingJar', runtime, developmentOnly)
                }
                else {
                    pathingJar = createPathingJarTask(project, 'pathingJar', runtime)
                }

                FileCollection pathingClasspath = project.files("${project.buildDir}/resources/main",
                        "${project.projectDir}/gsp-classes", pathingJar.getArchiveFile().get().getAsFile()) + mainFiles

                Jar pathingJarCommand = createPathingJarTask(project, 'pathingJarCommand', runtime, console)
                FileCollection pathingClasspathCommand = project.files("${project.buildDir}/resources/main",
                        "${project.projectDir}/gsp-classes", pathingJarCommand.getArchiveFile().get().getAsFile()) + mainFiles

                GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)

                if (grailsExt.isPathingJar() && Os.isFamily(Os.FAMILY_WINDOWS)) {
                    project.tasks.withType(JavaExec) { JavaExec task ->
                        if (task.name in ['console', 'shell']
                                || task instanceof ApplicationContextCommandTask
                                || task instanceof ApplicationContextScriptTask) {
                            task.dependsOn(pathingJarCommand)
                            task.doFirst {
                                classpath = pathingClasspathCommand
                            }
                        }
                        else {
                            task.dependsOn(pathingJar)
                            task.doFirst {
                                classpath = pathingClasspath
                            }
                        }
                    }
                }
            }
        }
    }

    protected void configureGroovyASTMetadata(Project project) {
        String projectName = getGrailsProjectName(project)
        String projectVersion = project.version
        String projectDir = project.projectDir.absolutePath
        GrailsProjectType projectType = getGrailsProjectType()
        File configFile = project.layout.buildDirectory.file('config.groovy').get().asFile

        TaskProvider<Task> configScriptTask = project.tasks.register('configScript') { Task configScript ->
            configScript.group = 'Build Setup'
            configScript.description = 'Generates Groovy configuration script.'

            configScript.outputs.file(configFile)
            configScript.inputs.property('name', projectName)
            configScript.inputs.property('version', projectVersion)
            configScript.doLast {
                String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
                String grailsAppDir = grailsAppPath ? project.file(grailsAppPath).absolutePath : ''
                if (System.getProperty('os.name').startsWith('Windows')) {
                    projectDir = projectDir.replace('\\', '\\\\')
                    grailsAppDir = grailsAppDir.replace('\\', '\\\\')
                }
                configFile.parentFile.mkdirs()
                configFile.text = """
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        source.ast.putNodeMetaData('GRAILS_APP_DIR', '$grailsAppDir')
        source.ast.putNodeMetaData('PROJECT_DIR', '$projectDir')
        source.ast.putNodeMetaData('PROJECT_NAME', '$projectName')
        source.ast.putNodeMetaData('PROJECT_TYPE', '$projectType')
        source.ast.putNodeMetaData('PROJECT_VERSION', '$projectVersion')
    }
}
"""
            }
        }
        project.tasks.named('compileGroovy', GroovyCompile).configure {
            it.dependsOn(configScriptTask)
            it.groovyOptions.configurationScript = configFile
        }
    }

    protected GrailsProjectType getGrailsProjectType() {
        GrailsProjectType.NONE
    }

    protected String getGrailsProjectName(Project project) {
        return project.name
    }

    protected FileCollection buildClasspath(Project project, Configuration... configurations) {
        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)
        SourceSetOutput output = mainSourceSet?.output
        FileCollection mainFiles = resolveClassesDirs(output, project)
        FileCollection fileCollection = project.files("${project.buildDir}/resources/main",
                "${project.projectDir}/gsp-classes") + mainFiles
        configurations.each {
            fileCollection = fileCollection + it.filter({ File file -> !file.name.startsWith('spring-boot-devtools') })
        }
        fileCollection
    }

    private void verifyGradleVersion() {
        GradleVersion currentVersion = GradleVersion.current()
        if (currentVersion < GradleVersion.version('7.6.4') ||
                (currentVersion >= GradleVersion.version('8.0') && currentVersion < GradleVersion.version('8.3'))) {
            throw new GradleException('Grace plugin requires Gradle 7.x (7.6.4 or later) or 8.x (8.3 or later). '
                    + 'The current version is ' + currentVersion)
        }
    }

    enum GrailsProjectType {
        NONE,

        WEB_APP,

        PLUGIN,

        PROFILE
    }

}
