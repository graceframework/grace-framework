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
package org.grails.gradle.plugin.core

import javax.inject.Inject

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun

import grails.cli.command.ApplicationCommand
import grails.util.Environment
import grails.util.GrailsNameUtils
import grails.util.Metadata

import org.grails.build.parsing.CommandLineParser
import org.grails.cli.compiler.dependencies.GrailsDependenciesDependencyManagement
import org.grails.gradle.plugin.commands.ApplicationContextCommandTask
import org.grails.gradle.plugin.commands.ApplicationContextScriptTask
import org.grails.gradle.plugin.model.GrailsClasspathToolingModelBuilder
import org.grails.gradle.plugin.run.FindMainClassTask
import org.grails.gradle.plugin.util.BuildSettings
import org.grails.gradle.plugin.util.SourceSets
import org.grails.io.support.GrailsFactoriesLoader

/**
 * The main Grails gradle plugin implementation
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GrailsGradlePlugin extends GroovyPlugin {

    public static final String GRAILS_EXTENSION_NAME = 'grails'
    public static final String CONSOLE_CONFIGURATION_NAME = 'console'
    public static final String PROFILE_CONFIGURATION_NAME = 'profile'
    public static final String FIND_MAIN_CLASS_TASK_NAME = 'findMainClass'
    public static final String BUILD_PROPERTIES_TASK_NAME = 'buildProperties'

    List<Class<Plugin>> basePluginClasses = [IntegrationTestGradlePlugin] as List<Class<Plugin>>

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

        configureSpringBootExtension(project)

        configureForkSettings(project, grailsVersion)

        configureGrailsSourceDirs(project)

        createBuildPropertiesTask(project)

        configureGroovyCompiler(project)
    }

    protected void configureProfile(Project project) {
        if (project.configurations.findByName(PROFILE_CONFIGURATION_NAME) == null) {
            Configuration profileConfiguration = project.configurations.create(PROFILE_CONFIGURATION_NAME)
            profileConfiguration.incoming.beforeResolve {
                if (!profileConfiguration.allDependencies) {
                    String profileDependency = "org.graceframework.profiles:${System.getProperty('grails.profile') ?: defaultProfile}:"
                    project.dependencies.add(PROFILE_CONFIGURATION_NAME, profileDependency)
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

    protected void createBuildPropertiesTask(Project project) {
        if (project.tasks.findByName(BUILD_PROPERTIES_TASK_NAME) == null) {
            File resourcesDir = SourceSets.findMainSourceSet(project).output.resourcesDir

            LinkedHashMap<String, Object> buildProperties = [
                    'grails.env': Environment.isSystemSet() ? Environment.current.getName() : Environment.PRODUCTION.getName(),
                    'info.app.name': project.name,
                    'info.app.version': project.version instanceof Serializable ? project.version : project.version.toString(),
                    'info.app.grailsVersion': grailsVersion]

            TaskProvider<GenerateBuildInfo> buildPropertiesTask = project.tasks.register(BUILD_PROPERTIES_TASK_NAME, GenerateBuildInfo)
                    { GenerateBuildInfo task ->
                task.group = 'build'
                task.description = "Build properties into 'META-INF/grails.build.info'."
                task.destinationDir.set(resourcesDir)
                task.properties.set(buildProperties)
            }
            project.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {it.dependsOn(buildPropertiesTask) }
        }
    }

    @CompileStatic
    protected void configureSpringBootExtension(Project project) {
        project.getTasks().withType(BootRun).configureEach { BootRun bootRun ->
            bootRun.doFirst("Configure System Properties") {
                String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
                bootRun.systemProperty(BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
                bootRun.systemProperty(BuildSettings.APP_DIR, grailsAppPath ? project.file(grailsAppPath).absolutePath : '')
                bootRun.systemProperty(BuildSettings.PROJECT_TARGET_DIR, SourceSets.getBuildTargetDir(project).absolutePath)
                bootRun.systemProperty(BuildSettings.PROJECT_RESOURCES_DIR, SourceSets.getBuildResourcesDir(project).absolutePath)
                bootRun.systemProperty(BuildSettings.PROJECT_CLASSES_DIR, SourceSets.getBuildClassesDir(project).absolutePath)
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
        if (project.extensions.findByName(GRAILS_EXTENSION_NAME) == null) {
            project.extensions.create(GrailsExtension, GRAILS_EXTENSION_NAME, GrailsExtension, project)
        }
    }

    protected void configureGrailsBuildSettings(Project project) {
        // We removed the configuration of system properties
        // because it causes errors when a project contains multiple plugin subprojects.
    }

    protected void configureApplicationCommands(Project project) {
        Configuration runtimeClasspath = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration consoleClasspath = project.configurations.getByName(CONSOLE_CONFIGURATION_NAME)
        Configuration profileClasspath = project.configurations.getByName(PROFILE_CONFIGURATION_NAME)

        URL[] urls = [project.layout.buildDirectory.dir('classes/groovy/main').get().asFile.toURI().toURL()]
        ClassLoader classLoader = new URLClassLoader(urls, GrailsFactoriesLoader.classLoader)
        List<ApplicationCommand> applicationContextCommands = GrailsFactoriesLoader.loadFactories(ApplicationCommand, classLoader)

        for (ApplicationCommand ctxCommand in applicationContextCommands) {
            String taskName = GrailsNameUtils.getLogicalPropertyName(ctxCommand.class.name, 'Command')
            String commandName = GrailsNameUtils.getScriptName(GrailsNameUtils.getLogicalName(ctxCommand.class.name, 'Command'))
            String commandDescription = ctxCommand.description
            String commandGroup = ctxCommand?.group ?: 'Command'
            project.tasks.register(taskName, ApplicationContextCommandTask) { ApplicationContextCommandTask commandTask ->
                commandTask.setGroup(commandGroup)
                commandTask.setDescription(commandDescription)
                commandTask.classpath = buildClasspath(project, runtimeClasspath, consoleClasspath, profileClasspath)
                commandTask.command = commandName
                commandTask.dependsOn(JavaPlugin.CLASSES_TASK_NAME, FIND_MAIN_CLASS_TASK_NAME)
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

    protected void configureGrailsSourceDirs(Project project) {
        List<File> grailsSourceDirs = []
        List<File> grailsResourceDirs = []
        project.afterEvaluate {
            GrailsExtension grailsExtension = project.extensions.getByType(GrailsExtension)
            String grailsAppPath = SourceSets.resolveGrailsAppPath(project)
            String[] grailsAppSourceDirs = grailsExtension.appSourceDirs
            String[] grailsAppResourceDirs = grailsExtension.appResourceDirs
            if (grailsAppPath) {
                for (String f in grailsAppSourceDirs) {
                    File subdir = project.file("${grailsAppPath}/${f}")
                    if (!subdir.hidden && !subdir.name.startsWith('.')) {
                        grailsSourceDirs.add(subdir)
                    }
                }
                for (String f in grailsAppResourceDirs) {
                    grailsResourceDirs.add(project.file("${grailsAppPath}/${f}"))
                }
                grailsSourceDirs.add(project.file('src/main/groovy'))
                grailsResourceDirs.add(project.file('src/main/resources'))
                SourceSet mainSourceSet = project.extensions.getByType(JavaPluginExtension).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                mainSourceSet.extensions.getByType(GroovySourceDirectorySet).setSrcDirs(grailsSourceDirs)
                mainSourceSet.getResources().setSrcDirs(grailsResourceDirs)
            }
        }
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
        Configuration runtimeClasspath = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        TaskContainer tasks = project.tasks

        if (project.configurations.findByName(CONSOLE_CONFIGURATION_NAME) == null) {
            Configuration consoleConfiguration = project.configurations.create(CONSOLE_CONFIGURATION_NAME)

            TaskProvider<JavaExec> consoleTask = tasks.register('console', JavaExec) { JavaExec console ->
                console.group = 'Grace'
                console.description = 'Runs the interactive Groovy Console.'
                console.systemProperty BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath
                console.systemProperty 'spring.devtools.restart.enabled', false
                console.systemProperty 'spring.output.ansi.enabled', 'always'
                console.classpath = buildClasspath(project, runtimeClasspath, consoleConfiguration)
                console.mainClass.set('grails.ui.console.GrailsConsole')
            }

            TaskProvider<JavaExec> shellTask = tasks.register('shell', JavaExec) { JavaExec shell ->
                shell.group = 'Grace'
                shell.description = 'Runs the interactive Groovy Shell.'
                shell.systemProperty BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath
                shell.systemProperty 'spring.devtools.restart.enabled', false
                shell.systemProperty 'spring.output.ansi.enabled', 'always'
                shell.classpath = buildClasspath(project, runtimeClasspath, consoleConfiguration)
                shell.mainClass.set('grails.ui.shell.GrailsShell')
                shell.standardInput = System.in
            }

            consoleTask.configure {
                it.dependsOn(tasks.named(JavaPlugin.CLASSES_TASK_NAME), tasks.withType(FindMainClassTask))
            }
            shellTask.configure {
                it.dependsOn(tasks.named(JavaPlugin.CLASSES_TASK_NAME), tasks.withType(FindMainClassTask))
            }

            tasks.withType(FindMainClassTask).configureEach {
                it.doLast {
                    ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) project.extensions.getByName('ext')
                    def mainClassName = extraProperties.get('mainClassName')
                    if (mainClassName) {
                        consoleTask.configure {
                            it.args mainClassName
                        }
                        shellTask.configure {
                            it.args mainClassName
                        }
                    }
                }
            }
        }
    }

    protected void registerFindMainClassTask(Project project) {
        Task findMainClassTask = project.tasks.findByName(FIND_MAIN_CLASS_TASK_NAME)
        if (findMainClassTask == null) {
            project.tasks.register(FIND_MAIN_CLASS_TASK_NAME, FindMainClassTask) { FindMainClassTask task ->
                task.group = 'build'
                task.description = 'Finds the main class of the application.'
                task.dependsOn(project.tasks.named(JavaPlugin.CLASSES_TASK_NAME))
            }
        }
        else if (!FindMainClassTask.isAssignableFrom(findMainClassTask.class)) {
            project.tasks.register('grailsFindMainClass', FindMainClassTask) { FindMainClassTask task ->
                task.group = 'build'
                task.description = 'Finds the main class of the application.'
                task.dependsOn(project.tasks.named(JavaPlugin.CLASSES_TASK_NAME), findMainClassTask)
                findMainClassTask.finalizedBy(task)
            }
        }
    }

    protected void configureRunScript(Project project) {
        Configuration runtimeClasspath = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration consoleClasspath = project.configurations.getByName(CONSOLE_CONFIGURATION_NAME)
        Configuration profileClasspath = project.configurations.getByName(PROFILE_CONFIGURATION_NAME)

        if (project.tasks.findByName('runScript') == null) {
            project.tasks.register('runScript', ApplicationContextScriptTask) { ApplicationContextScriptTask scriptTask ->
                scriptTask.group = 'Grace'
                scriptTask.description = "Executes the Grace Application Scripts."
                scriptTask.classpath = buildClasspath(project, runtimeClasspath, consoleClasspath, profileClasspath)
                scriptTask.dependsOn(JavaPlugin.CLASSES_TASK_NAME, FIND_MAIN_CLASS_TASK_NAME)
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

            project.tasks.withType(FindMainClassTask).configureEach { Task findMainClass ->
                findMainClass.doLast {
                    ExtraPropertiesExtension extraProperties = project.extensions.getByType(ExtraPropertiesExtension)
                    def mainClassName = extraProperties.get('mainClassName')
                    if (mainClassName) {
                        project.tasks.withType(ApplicationContextScriptTask).configureEach { ApplicationContextScriptTask scriptTask ->
                            scriptTask.args mainClassName
                        }
                    }
                }
            }
        }
    }

    protected void configureRunCommand(Project project) {
        Configuration runtimeClasspath = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration consoleClasspath = project.configurations.getByName(CONSOLE_CONFIGURATION_NAME)
        Configuration profileClasspath = project.configurations.getByName(PROFILE_CONFIGURATION_NAME)

        if (project.tasks.findByName('runCommand') == null) {
            project.tasks.register('runCommand', ApplicationContextCommandTask) { ApplicationContextCommandTask commandTask ->
                commandTask.group = 'Grace'
                commandTask.description = "Executes the Grace Application Commands."
                commandTask.classpath = buildClasspath(project, runtimeClasspath, consoleClasspath, profileClasspath)
                commandTask.dependsOn(JavaPlugin.CLASSES_TASK_NAME, FIND_MAIN_CLASS_TASK_NAME)
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

            project.tasks.withType(FindMainClassTask).configureEach { Task findMainClass ->
                findMainClass.doLast {
                    ExtraPropertiesExtension extraProperties = project.extensions.getByType(ExtraPropertiesExtension)
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

    protected void configurePathingJar(Project project) {
        ConfigurationContainer configurations = project.configurations
        Configuration runtime = configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        Configuration developmentOnly = configurations.findByName(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME)
        Configuration console = configurations.getByName(CONSOLE_CONFIGURATION_NAME)

        Jar pathingJar = (Jar) project.tasks.findByName('pathingJar')
        Jar pathingJarCommand = (Jar) project.tasks.findByName('pathingJarCommand')

        project.afterEvaluate {
            if (pathingJar == null) {
                if (developmentOnly != null) {
                    pathingJar = createPathingJarTask(project, 'pathingJar', runtime, developmentOnly)
                }
                else {
                    pathingJar = createPathingJarTask(project, 'pathingJar', runtime)
                }

                if (pathingJarCommand == null) {
                    pathingJarCommand = createPathingJarTask(project, 'pathingJarCommand', runtime, console)
                }

                FileCollection pathingClasspath = project.files(buildClasspath(project), pathingJar.getArchiveFile().get().getAsFile())
                FileCollection pathingClasspathCommand = project.files(buildClasspath(project), pathingJarCommand.getArchiveFile().get().getAsFile())

                GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)

                if (grailsExt.isPathingJar() && Os.isFamily(Os.FAMILY_WINDOWS)) {
                    project.tasks.withType(JavaExec).configureEach { JavaExec task ->
                        if (task.name in ['console', 'shell']
                                || task instanceof ApplicationContextCommandTask
                                || task instanceof ApplicationContextScriptTask) {
                            task.dependsOn(pathingJarCommand)
                            task.doFirst {
                                task.classpath = pathingClasspathCommand
                            }
                        }
                        else {
                            task.dependsOn(pathingJar)
                            task.doFirst {
                                task.classpath = pathingClasspath
                            }
                        }
                    }
                }
            }
        }
    }

    protected Jar createPathingJarTask(Project project, String name, Configuration... configurations) {
        project.tasks.create(name, Jar) { Jar task ->
            task.group = 'build'
            task.description = 'Generates a pathing jar file.'
            task.archiveAppendix.set('pathing')

            Set<File> files = []
            configurations.each {
                files.addAll(it.files)
            }

            task.doFirst {
                task.manifest { Manifest manifest ->
                    task.manifest.attributes 'Class-Path': files.collect { File file ->
                        file.toURI().toURL().toString().replaceFirst(/file:\/+/, '/')
                    }.join(' ')
                }
            }
        }
    }

    protected void configureGroovyCompiler(Project project) {
        File configFile = project.layout.buildDirectory.file('config.groovy').get().asFile

        TaskProvider<GenerateConfigScript> configScriptTask = project.tasks.register('configScript', GenerateConfigScript) {
            GenerateConfigScript configScript ->
            configScript.group = 'Build Setup'
            configScript.description = 'Generates Groovy configuration script.'
            configScript.configFile.set(configFile)
            configScript.projectDir.set(project.projectDir.absolutePath)
            configScript.projectType.set(getGrailsProjectType().toString())
            configScript.projectVersion.set(project.getVersion().toString())
            configScript.grailsAppDir.set(SourceSets.resolveGrailsAppPath(project) ? project.file(SourceSets.resolveGrailsAppPath(project)).absolutePath : null)
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
        FileCollection fileCollection = project.files(project.layout.buildDirectory.dir('resources/main'),
                project.layout.buildDirectory.dir('classes/gsp/main'), mainFiles)
        configurations.each {
            fileCollection = fileCollection + it.filter({ File file -> !file.name.startsWith('spring-boot-devtools') })
        }
        fileCollection
    }

    private FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(project.layout.buildDirectory.dir('classes/groovy/main'))
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
