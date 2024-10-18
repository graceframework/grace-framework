/*
 * Copyright 2015-2024 the original author or authors.
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
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.JavaForkOptions
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
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

    public static final String PROFILE_CONFIGURATION = 'profile'

    List<Class<Plugin>> basePluginClasses = [IntegrationTestGradlePlugin] as List<Class<Plugin>>
    List<String> excludedGrailsAppSourceDirs = ['assets', 'scripts']
    List<String> grailsAppResourceDirs = ['i18n', 'conf']
    private final ToolingModelBuilderRegistry registry
    String grailsAppDir
    String grailsVersion

    @Inject
    GrailsGradlePlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry
    }

    void apply(Project project) {
        grailsAppDir = SourceSets.resolveGrailsAppDir(project)
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

        registerGrailsExtension(project)

        applyBasePlugins(project)

        registerFindMainClassTask(project)

        configureGrailsBuildSettings(project)

        enableNative2Ascii(project, grailsVersion)

        configureSpringBootExtension(project)

        configureAssetCompilation(project)

        configureConsoleTask(project)

        configureForkSettings(project, grailsVersion)

        configureGrailsSourceDirs(project)

        configureApplicationCommands(project)

        createBuildPropertiesTask(project)

        configureRunScript(project)

        configureRunCommand(project)

        configurePathingJar(project)

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
        String springBootVersion = resolveSpringBootVersion(project)
        dme.imports({
            mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
            mavenBom("org.graceframework:grace-bom:${grailsVersion}")
        })
        dme.setApplyMavenExclusions(false)
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
        project.getTasks().withType(BootRun, {
            systemProperty(BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
            systemProperty(BuildSettings.APP_DIR, project.file(grailsAppDir).absolutePath)
            systemProperty(BuildSettings.PROJECT_TARGET_DIR, project.buildDir.absolutePath)
            systemProperty(BuildSettings.PROJECT_RESOURCES_DIR, new File(project.buildDir, 'resources/main').absolutePath)
            systemProperty(BuildSettings.PROJECT_CLASSES_DIR, new File(project.buildDir, 'classes/groovy/main').absolutePath)
        })
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
            project.extensions.add('grails', new GrailsExtension(project))
        }
    }

    @CompileStatic
    protected String configureGrailsBuildSettings(Project project) {
        System.setProperty(BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
        System.setProperty(BuildSettings.APP_DIR, project.file(grailsAppDir).absolutePath)
        System.setProperty(BuildSettings.PROJECT_TARGET_DIR, project.buildDir.absolutePath)
        System.setProperty(BuildSettings.PROJECT_RESOURCES_DIR, new File(project.buildDir, 'resources/main').absolutePath)
        System.setProperty(BuildSettings.PROJECT_CLASSES_DIR, new File(project.buildDir, 'classes/groovy/main').absolutePath)
    }

    @CompileDynamic
    protected void configureApplicationCommands(Project project) {
        URL[] urls = [new File(project.buildDir, 'classes/groovy/main').toURI().toURL()]
        ClassLoader classLoader = new URLClassLoader(urls, GrailsFactoriesLoader.classLoader)
        List<ApplicationCommand> applicationContextCommands = GrailsFactoriesLoader.loadFactories(ApplicationCommand, classLoader)
        project.afterEvaluate {
            FileCollection fileCollection = buildClasspath(project, project.configurations.runtimeClasspath, project.configurations.console,
                    project.configurations.profile)
            for (ApplicationCommand ctxCommand in applicationContextCommands) {
                String taskName = GrailsNameUtils.getLogicalPropertyName(ctxCommand.class.name, 'Command')
                String commandName = GrailsNameUtils.getScriptName(GrailsNameUtils.getLogicalName(ctxCommand.class.name, 'Command'))
                String commandDescription = ctxCommand.description
                project.tasks.register(taskName, ApplicationContextCommandTask, { commandTask ->
                    commandTask.setGroup("Command")
                    commandTask.setDescription(commandDescription)
                    commandTask.classpath = fileCollection
                    commandTask.command = commandName
                    systemProperty 'spring.main.banner-mode', 'OFF'
                    systemProperty 'logging.level.ROOT', 'OFF'
                    systemProperty 'spring.output.ansi.enabled', 'always'
                    systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
                    if (project.hasProperty('args')) {
                        commandTask.args(CommandLineParser.translateCommandline(project.args))
                    }
                })
            }
        }
    }

    @CompileDynamic
    protected void configureGrailsSourceDirs(Project project) {
        project.sourceSets {
            main {
                groovy {
                    srcDirs = resolveGrailsSourceDirs(project)
                }
                resources {
                    srcDirs = resolveGrailsResourceDirs(project)
                }
            }
        }
    }

    @CompileStatic
    protected List<File> resolveGrailsResourceDirs(Project project) {
        List<File> grailsResourceDirs = [project.file('src/main/resources')]
        for (String f in grailsAppResourceDirs) {
            grailsResourceDirs.add(project.file("${grailsAppDir}/${f}"))
        }
        grailsResourceDirs
    }

    @CompileStatic
    protected List<File> resolveGrailsSourceDirs(Project project) {
        List<File> grailsSourceDirs = []
        project.file(grailsAppDir).eachDir { File subdir ->
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

    protected String resolveSpringBootVersion(Project project) {
        def springBootVersion = project.findProperty('springBootVersion')

        springBootVersion = springBootVersion ?: new GrailsDependenciesDependencyManagement().getSpringBootVersion()

        springBootVersion
    }

    protected String resolveSpringFrameworkVersion(Project project) {
        def springFrameworkVersion = project.findProperty('springFrameworkVersion') ?: project.findProperty('springVersion')

        springFrameworkVersion = springFrameworkVersion ?: new GrailsDependenciesDependencyManagement().find('spring-framework-bom').getVersion()

        springFrameworkVersion
    }

    protected String resolveTomcatVersion(Project project) {
        def tomcatVersion = project.findProperty('tomcatVersion')

        tomcatVersion = tomcatVersion ?: new GrailsDependenciesDependencyManagement().find('tomcat-embed-core').getVersion()

        tomcatVersion
    }

    @CompileDynamic
    protected void configureAssetCompilation(Project project) {
        if (project.extensions.findByName('assets')) {
            project.assets {
                assetsPath = "${grailsAppDir}/assets"
            }
        }
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
            findMainClass.group = 'build'
            findMainClass.description = 'Finds the main class of the application.'
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
            systemProperty BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath
            group = 'Grace'
            description = 'Runs the interactive Groovy Console.'
            classpath = project.sourceSets.main.runtimeClasspath + configuration
            mainClass.set('grails.ui.console.GrailsConsole')
        }
    }

    @CompileDynamic
    protected JavaExec createShellTask(Project project, TaskContainer tasks, Configuration configuration) {
        tasks.create('shell', JavaExec) {
            systemProperty BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath
            group = 'Grace'
            description = 'Runs the interactive Groovy Shell.'
            classpath = project.sourceSets.main.runtimeClasspath + configuration
            mainClass.set('grails.ui.shell.GrailsShell')
            standardInput = System.in
        }
    }

    protected void registerFindMainClassTask(Project project) {
        TaskContainer taskContainer = project.tasks
        def findMainClassTask = taskContainer.findByName('findMainClass')
        if (findMainClassTask == null) {
            findMainClassTask = project.tasks.register('findMainClass', FindMainClassTask).get()
            findMainClassTask.mustRunAfter(project.tasks.withType(GroovyCompile))
        }
        else if (!FindMainClassTask.isAssignableFrom(findMainClassTask.class)) {
            def grailsFindMainClass = project.tasks.register('grailsFindMainClass', FindMainClassTask).get()
            grailsFindMainClass.dependsOn(findMainClassTask)
            findMainClassTask.finalizedBy(grailsFindMainClass)
        }
    }

    /**
     * Enables native2ascii processing of resource bundles
     **/
    @CompileDynamic
    protected void enableNative2Ascii(Project project, String grailsVersion) {
        project.afterEvaluate {
            SourceSet sourceSet = SourceSets.findMainSourceSet(project)

            TaskContainer taskContainer = project.tasks

            taskContainer.getByName(sourceSet.processResourcesTaskName) { AbstractCopyTask task ->
                GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)
                boolean native2ascii = grailsExt.isNative2ascii()
                task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
                if (native2ascii && grailsExt.isNative2asciiAnt() && !taskContainer.findByName('native2ascii')) {
                    File destinationDir = ((ProcessResources) task).destinationDir
                    Task native2asciiTask = createNative2AsciiTask(taskContainer, project.file("${grailsAppDir}/i18n"), destinationDir)
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

    @CompileDynamic
    protected void configureRunScript(Project project) {
        if (project.tasks.findByName('runScript') == null) {
            project.tasks.create('runScript', ApplicationContextScriptTask) {
                group = 'Grace'
                description = "Executes the Grace Application Scripts."
                classpath = buildClasspath(project, project.configurations.runtimeClasspath, project.configurations.console,
                        project.configurations.profile)
                systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
                systemProperty BuildSettings.APP_BASE_DIR, project.projectDir
                systemProperty 'spring.main.banner-mode', 'OFF'
                systemProperty 'logging.level.ROOT', 'OFF'
                systemProperty 'spring.output.ansi.enabled', 'always'
                if (project.hasProperty('args')) {
                    args(CommandLineParser.translateCommandline(project.args))
                }
            }
        }
    }

    @CompileDynamic
    protected void configureRunCommand(Project project) {
        if (project.tasks.findByName('runCommand') == null) {
            def findMainClass = project.tasks.findByName('findMainClass')
            findMainClass.doLast {
                ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) project.getExtensions().getByName('ext')
                def mainClassName = extraProperties.get('mainClassName')
                if (mainClassName) {
                    project.tasks.withType(ApplicationContextCommandTask) { ApplicationContextCommandTask task ->
                        task.args mainClassName
                    }
                }
            }
            project.tasks.create('runCommand', ApplicationContextCommandTask) {
                group = 'Grace'
                description = "Executes the Grace Application Commands."
                classpath = buildClasspath(project, project.configurations.runtimeClasspath, project.configurations.console,
                        project.configurations.profile)
                systemProperty Environment.KEY, System.getProperty(Environment.KEY, Environment.DEVELOPMENT.getName())
                systemProperty BuildSettings.APP_BASE_DIR, project.projectDir
                systemProperty 'spring.main.banner-mode', 'OFF'
                systemProperty 'logging.level.ROOT', 'OFF'
                systemProperty 'spring.output.ansi.enabled', 'always'
                if (project.hasProperty('args')) {
                    args(CommandLineParser.translateCommandline(project.args))
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

    @CompileDynamic
    protected void configureGroovyASTMetadata(Project project) {
        def configScriptTask = project.tasks.create('configScript')
        configScriptTask.group = 'Build Setup'
        configScriptTask.description = 'Generates Groovy configuration script.'

        def configFile = project.file("$project.buildDir/config.groovy")
        configScriptTask.outputs.file(configFile)

        def projectName = project.name
        def projectVersion = project.version
        def projectDir = project.projectDir.absolutePath
        def projectType = getGrailsProjectType()
        def isPlugin = projectType == GrailsProjectType.PLUGIN
        def grailsAppDir = new File(project.projectDir, grailsAppDir).absolutePath
        if (System.getProperty('os.name').startsWith('Windows')) {
            projectDir = projectDir.replace('\\', '\\\\')
            grailsAppDir = grailsAppDir.replace('\\', '\\\\')
        }
        configScriptTask.inputs.property('name', projectName)
        configScriptTask.inputs.property('version', projectVersion)
        configScriptTask.doLast {
            configFile.parentFile.mkdirs()
            configFile.text = """
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        source.ast.putNodeMetaData('GRAILS_APP_DIR', '$grailsAppDir')
        source.ast.putNodeMetaData('PROJECT_DIR', '$projectDir')
        source.ast.putNodeMetaData('PROJECT_NAME', '$projectName')
        source.ast.putNodeMetaData('PROJECT_TYPE', '$projectType')
        source.ast.putNodeMetaData('PROJECT_VERSION', '$projectVersion')
        classNode.putNodeMetaData('projectVersion', '$projectVersion')
        classNode.putNodeMetaData('projectName', '$projectName')
        classNode.putNodeMetaData('isPlugin', '$isPlugin')
    }
}
"""
        }
        project.tasks.getByName('compileGroovy').dependsOn(configScriptTask)
        project.compileGroovy {
            groovyOptions.configurationScript = configFile
        }
    }

    protected GrailsProjectType getGrailsProjectType() {
        GrailsProjectType.NONE
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

    enum GrailsProjectType {
        NONE,

        WEB_APP,

        PLUGIN,

        PROFILE
    }

}
