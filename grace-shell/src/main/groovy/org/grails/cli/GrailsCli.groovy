/*
 * Copyright 2014-2024 the original author or authors.
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
package org.grails.cli

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jline.Terminal
import jline.UnixTerminal
import jline.console.ConsoleReader
import jline.console.UserInterruptException
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
import jline.console.history.History
import jline.internal.NonBlockingInputStream
import org.gradle.tooling.BuildActionExecuter
import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.ProgressEvent
import org.gradle.tooling.ProgressListener
import org.gradle.tooling.ProjectConnection
import org.gradle.util.internal.DefaultGradleVersion

import grails.build.logging.GrailsConsole
import grails.build.proxy.SystemPropertiesAuthenticator
import grails.config.ConfigMap
import grails.io.support.SystemStreamsRedirector
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsVersion

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.grails.build.parsing.DefaultCommandLine
import org.grails.cli.compiler.dependencies.GrailsDependenciesDependencyManagement
import org.grails.cli.gradle.ClasspathBuildAction
import org.grails.cli.gradle.GradleAsyncInvoker
import org.grails.cli.gradle.cache.MapReadingCachedGradleOperation
import org.grails.cli.interactive.completers.EscapingFileNameCompletor
import org.grails.cli.interactive.completers.RegexCompletor
import org.grails.cli.interactive.completers.SortedAggregateCompleter
import org.grails.cli.interactive.completers.StringsCompleter
import org.grails.cli.profile.Command
import org.grails.cli.profile.CommandArgument
import org.grails.cli.profile.CommandCancellationListener
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProjectContext
import org.grails.cli.profile.commands.CommandCompleter
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.cli.profile.repository.GrailsRepositoryConfiguration
import org.grails.cli.profile.repository.MavenProfileRepository
import org.grails.cli.profile.repository.StaticJarProfileRepository
import org.grails.config.CodeGenConfig
import org.grails.config.NavigableMap
import org.grails.exceptions.ExceptionUtils
import org.grails.gradle.plugin.model.GrailsClasspath

/**
 * Main class for the Grails command line.
 * Handles interactive mode and running Grails commands within the context of a profile
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 * @author Michael Yan
 *
 * @since 3.0
 */
@CompileStatic
class GrailsCli {

    static final String ARG_SPLIT_PATTERN = /(?<!\\)\s+/
    public static final String DEFAULT_PROFILE_NAME = ProfileRepository.DEFAULT_PROFILE_NAME
    private static final int KEYPRESS_CTRL_C = 3
    private static final int KEYPRESS_ESC = 27
    private static final String USAGE_MESSAGE = 'create-app [NAME] --profile=web'
    private static final String PLUGIN_USAGE_MESSAGE = 'create-plugin [NAME] --profile=web-plugin'
    private final SystemStreamsRedirector originalStreams = SystemStreamsRedirector.original() // store original System.in, System.out and System.err
    private static ExecutionContext currentExecutionContext = null

    private static boolean interactiveModeActive
    private static boolean triggerAppLoad = false
    private static final NavigableMap SETTINGS_MAP = new NavigableMap()

    static {
        if (BuildSettings.SETTINGS_FILE.exists()) {
            try {
                SETTINGS_MAP.merge new ConfigSlurper().parse(BuildSettings.SETTINGS_FILE.toURI().toURL())
            }
            catch (Throwable e) {
                e.printStackTrace()
                System.err.println("ERROR: Problem loading $BuildSettings.SETTINGS_FILE: ${e.message}")
            }

            try {
                Runtime.addShutdownHook {
                    try {
                        Thread.start {
                            currentExecutionContext?.cancel()
                        }.join(1000)
                    }
                    catch (Throwable ignored) {
                    }
                }
            }
            catch (ignored) {
            }
        }
    }

    SortedAggregateCompleter aggregateCompleter = new SortedAggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    boolean keepRunning = true
    Boolean ansiEnabled = null
    boolean integrateGradle = true
    Character defaultInputMask = null
    ProfileRepository profileRepository
    CodeGenConfig applicationConfig
    ProjectContext projectContext
    Profile profile = null
    List<GrailsRepositoryConfiguration> profileRepositories = [
            MavenProfileRepository.MAVEN_LOCAL_REPO,
            MavenProfileRepository.DEFAULT_REPO
    ]

    /**
     * Obtains a value from USER_HOME/.grails/settings.yml
     *
     * @param key the property name to resolve
     * @param targetType the expected type of the property value
     * @param defaultValue The default value
     */
    static <T> T getSetting(String key, Class<T> targetType = Object, T defaultValue = null) {
        Object value = SETTINGS_MAP.get(key, defaultValue)
        if (value == null) {
            return null
        }
        else if (targetType.isInstance(value)) {
            return (T) value
        }

        try {
            return value.asType(targetType)
        }
        catch (Throwable ignored) {
            return null
        }
    }

    /**
     * Main method for running via the command line
     *
     * @param args The arguments
     */
    static void main(String[] args) {
        Authenticator.setDefault(getSetting(BuildSettings.AUTHENTICATOR, Authenticator, new SystemPropertiesAuthenticator()))
        ProxySelector proxySelector = getSetting(BuildSettings.PROXY_SELECTOR, ProxySelector)
        if (proxySelector != null) {
            ProxySelector.setDefault(proxySelector)
        }

        GrailsCli cli = new GrailsCli()
        try {
            exit(cli.execute(args))
        }
        catch (BuildCancelledException ignored) {
            GrailsConsole.instance.addStatus('Build stopped.')
            exit(0)
        }
        catch (Throwable e) {
            e = ExceptionUtils.getRootCause(e)
            GrailsConsole.instance.error("Error occurred running Grace CLI: $e.message", e)
            exit(1)
        }
    }

    static void exit(int code) {
        GrailsConsole.instance.cleanlyExit(code)
    }

    static boolean isInteractiveModeActive() {
        interactiveModeActive
    }

    static void triggerAppLoad() {
        GrailsCli.triggerAppLoad = true
    }

    private int getBaseUsage() {
        System.out.println "Usage: \n\t $USAGE_MESSAGE \n\t $PLUGIN_USAGE_MESSAGE \n\n"
        this.execute 'list-profiles'
        System.out.println "\nType 'grace help' or 'grace -h' for more information."

        1
    }

    /**
     * Execute the given command
     *
     * @param args The arguments
     * @return The exit status code
     */
    int execute(String... args) {
        CommandLine mainCommandLine = cliParser.parse(args)

        if (mainCommandLine.hasOption(CommandLine.VERBOSE_ARGUMENT)) {
            System.setProperty('grails.verbose', 'true')
            System.setProperty('grails.full.stacktrace', 'true')
        }
        if (mainCommandLine.hasOption(CommandLine.STACKTRACE_ARGUMENT)) {
            System.setProperty('grails.show.stacktrace', 'true')
        }

        if (mainCommandLine.hasOption(CommandLine.VERSION_ARGUMENT) || mainCommandLine.hasOption('v')) {
            GrailsVersion currentVersion = GrailsVersion.current()
            GrailsDependenciesDependencyManagement grailsDependencies = new GrailsDependenciesDependencyManagement()
            StringBuilder sb = new StringBuilder()
            sb.append('%n------------------------------------------------------------%nGrace ')
            sb.append(currentVersion.getVersion())
            sb.append('%n------------------------------------------------------------%n%nBuild time:   ')
            sb.append(currentVersion.getBuildTimestamp())
            sb.append('%nRevision:     ')
            sb.append(currentVersion.getGitRevision())
            sb.append('%n%nSpring Boot:  ')
            sb.append(grailsDependencies.getSpringBootVersion())
            sb.append('%nGradle:       ')
            sb.append(DefaultGradleVersion.current().getVersion())
            sb.append('%nGroovy:       ')
            sb.append(grailsDependencies.getGroovyVersion())
            sb.append('%nJVM:          ')
            sb.append(String.format('%s (%s %s)', System.getProperty('java.version'),
                    System.getProperty('java.vm.vendor'), System.getProperty('java.vm.version')))
            sb.append('%nOS:           ')
            sb.append(String.format('%s %s %s', System.getProperty('os.name'), System.getProperty('os.version'), System.getProperty('os.arch')))
            sb.append('%n%n')
            GrailsConsole console = GrailsConsole.instance
            console.log(String.format(sb.toString()))
            exit(0)
        }

        if (mainCommandLine.hasOption(CommandLine.HELP_ARGUMENT) || mainCommandLine.hasOption('h')) {
            profileRepository = createMavenProfileRepository()
            Command cmd = CommandRegistry.getCommand('help', profileRepository)
            cmd.handle(createExecutionContext(mainCommandLine))
            exit(0)
        }

        if (mainCommandLine.environmentSet) {
            System.setProperty(Environment.KEY, mainCommandLine.environment)
            Environment.reset()
        }

        boolean grailsAppDirPresent = BuildSettings.GRAILS_APP_DIR_PRESENT
        File applicationGroovy = new File('Application.groovy')
        File profileYml = new File('profile.yml')
        if (!grailsAppDirPresent && !applicationGroovy.exists() && !profileYml.exists()) {
            profileRepository = createMavenProfileRepository()
            if (!mainCommandLine || !mainCommandLine.commandName) {
                integrateGradle = false
                GrailsConsole console = GrailsConsole.getInstance()
                // force resolve of all profiles
                profileRepository.getAllProfiles()
                List<String> commandNames = CommandRegistry.findCommands(profileRepository)*.fullName
                console.reader.addCompleter(new StringsCompleter(commandNames))
                console.reader.addCompleter(new CommandCompleter(CommandRegistry.findCommands(profileRepository)))
                profile = [handleCommand: { ExecutionContext context ->
                    CommandLine cl = context.commandLine
                    String name = cl.commandName
                    Command cmd = CommandRegistry.getCommand(name, profileRepository)
                    if (cmd != null) {
                        return executeCommandWithArgumentValidation(cmd, cl)
                    }
                    console.error("Command not found [$name]")
                    false
                }] as Profile

                startInteractiveMode(console)
                return 0
            }
            Command cmd = CommandRegistry.getCommand(mainCommandLine.commandName, profileRepository)
            if (cmd) {
                return executeCommandWithArgumentValidation(cmd, mainCommandLine) ? 0 : 1
            }
            getBaseUsage()
        }
        else {
            initializeApplication(mainCommandLine)
            if (mainCommandLine.commandName) {
                return handleCommand(mainCommandLine) ? 0 : 1
            }
            handleInteractiveMode()
        }
        0
    }

    protected boolean executeCommandWithArgumentValidation(Command cmd, CommandLine mainCommandLine) {
        Collection<CommandArgument> arguments = cmd.description.arguments
        Number requiredArgs = arguments.count { CommandArgument arg -> arg.required }
        if (mainCommandLine.remainingArgs.size() < requiredArgs) {
            outputMissingArgumentsMessage cmd
            return false
        }
        if (cmd.isDeprecated()) {
            GrailsConsole.getInstance().warning("Command [$cmd.fullName] is deprecated, and will be removed in the future release.")
        }
        cmd.handle(createExecutionContext(mainCommandLine))
    }

    protected void initializeApplication(CommandLine mainCommandLine) {
        applicationConfig = loadApplicationConfig()
        File profileYml = new File('profile.yml')
        if (profileYml.exists()) {
            // use the profile for profiles
            applicationConfig.put(BuildSettings.PROFILE, 'profile')
        }

        GrailsConsole console = GrailsConsole.instance
        console.ansiEnabled = !mainCommandLine.hasOption(CommandLine.NOANSI_ARGUMENT)
        console.defaultInputMask = defaultInputMask
        if (ansiEnabled != null) {
            console.ansiEnabled = ansiEnabled
        }
        File baseDir = new File('.').canonicalFile
        projectContext = new ProjectContextImpl(console, baseDir, applicationConfig)
        initializeProfile()
    }

    protected MavenProfileRepository createMavenProfileRepository() {
        Map profileRepos = getSetting(BuildSettings.PROFILE_REPOSITORIES, Map, Collections.emptyMap())
        if (!profileRepos.isEmpty()) {
            profileRepositories.clear()
            for (repoName in profileRepos.keySet()) {
                Object data = profileRepos.get(repoName)
                if (data instanceof Map) {
                    Object uri = data.get('url')
                    Object snapshots = data.get('snapshotsEnabled')
                    if (uri != null) {
                        boolean enableSnapshots = snapshots != null ? Boolean.valueOf(snapshots.toString()) : false
                        GrailsRepositoryConfiguration repositoryConfiguration
                        String username = data.get('username')
                        String password = data.get('password')
                        if (username != null && password != null) {
                            repositoryConfiguration = new GrailsRepositoryConfiguration(repoName.toString(),
                                    new URI(uri.toString()), enableSnapshots, username, password)
                        }
                        else {
                            repositoryConfiguration = new GrailsRepositoryConfiguration(repoName.toString(),
                                    new URI(uri.toString()), enableSnapshots)
                        }
                        profileRepositories.add(repositoryConfiguration)
                    }
                }
            }
        }
        new MavenProfileRepository(profileRepositories)
    }

    protected void outputMissingArgumentsMessage(Command cmd) {
        GrailsConsole console = GrailsConsole.instance
        console.error("Command $cmd.name is missing required arguments:")
        for (CommandArgument arg in cmd.description.arguments.findAll { CommandArgument ca -> ca.required }) {
            console.log("* $arg.name - $arg.description")
        }
    }

    ExecutionContext createExecutionContext(CommandLine commandLine) {
        new ExecutionContextImpl(commandLine, projectContext)
    }

    Boolean handleCommand(CommandLine commandLine) {
        handleCommand(createExecutionContext(commandLine))
    }

    Boolean handleCommand(ExecutionContext context) {
        GrailsConsole console = GrailsConsole.getInstance()
        synchronized (GrailsCli) {
            try {
                currentExecutionContext = context
                if (handleBuiltInCommands(context)) {
                    return true
                }

                CommandLine mainCommandLine = context.getCommandLine()
                if (mainCommandLine.hasOption(CommandLine.STACKTRACE_ARGUMENT)) {
                    console.setStacktrace(true)
                }
                else {
                    console.setStacktrace(false)
                }

                if (mainCommandLine.hasOption(CommandLine.VERBOSE_ARGUMENT)) {
                    System.setProperty('grails.verbose', 'true')
                    System.setProperty('grails.full.stacktrace', 'true')
                }
                else {
                    System.setProperty('grails.verbose', 'false')
                    System.setProperty('grails.full.stacktrace', 'false')
                }
                if (profile.handleCommand(context)) {
                    if (triggerAppLoad) {
                        console.updateStatus('Initializing application. Please wait...')
                        try {
                            initializeApplication(context.commandLine)
                            setupCompleters()
                        }
                        finally {
                            triggerAppLoad = false
                        }
                    }
                    return true
                }
                return false
            }
            catch (Throwable e) {
                console.error("Command [${context.commandLine.commandName}] error: ${e.message}", e)
                return false
            }
            finally {
                currentExecutionContext = null
            }
        }
    }

    private void handleInteractiveMode() {
        GrailsConsole console = setupCompleters()
        startInteractiveMode(console)
    }

    protected GrailsConsole setupCompleters() {
        System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, 'true')
        GrailsConsole console = projectContext.console

        ConsoleReader consoleReader = console.reader
        consoleReader.setHandleUserInterrupt(true)
        Collection<Completer> completers = aggregateCompleter.getCompleters()

        console.resetCompleters()
        // add bang operator completer
        completers.add(new ArgumentCompleter(
                new RegexCompletor('!\\w+'), new EscapingFileNameCompletor())
        )

        completers.addAll((profile.getCompleters(projectContext) ?: []) as Collection<Completer>)
        consoleReader.addCompleter(aggregateCompleter)
        console
    }

    protected void startInteractiveMode(GrailsConsole console) {
        console.updateStatus('Starting interactive mode...')
        ExecutorService commandExecutor = Executors.newFixedThreadPool(1)
        try {
            interactiveModeLoop(console, commandExecutor)
        }
        finally {
            commandExecutor.shutdownNow()
        }
    }

    private void interactiveModeLoop(GrailsConsole console, ExecutorService commandExecutor) {
        NonBlockingInputStream nonBlockingInput = (NonBlockingInputStream) console.reader.getInput()
        interactiveModeActive = true
        boolean firstRun = true
        while (keepRunning) {
            try {
                if (firstRun) {
                    console.addStatus('Enter a command name to run. Use TAB for completion:')
                    firstRun = false
                }
                String commandLine = console.showPrompt()
                if (commandLine == null) {
                    // CTRL-D was pressed, exit interactive mode
                    exitInteractiveMode()
                }
                else if (commandLine.trim()) {
                    if (nonBlockingInput.isNonBlockingEnabled()) {
                        handleCommandWithCancellationSupport(console, commandLine, commandExecutor, nonBlockingInput)
                    }
                    else {
                        handleCommand(cliParser.parseString(commandLine))
                    }
                }
            }
            catch (BuildCancelledException ignored) {
                console.updateStatus('Build stopped.')
            }
            catch (UserInterruptException ignored) {
                exitInteractiveMode()
            }
            catch (Throwable e) {
                console.error "Caught exception ${e.message}", e
            }
        }
    }

    private Boolean handleCommandWithCancellationSupport(GrailsConsole console, String commandLine,
                                                         ExecutorService commandExecutor, NonBlockingInputStream nonBlockingInput) {
        ExecutionContext executionContext = createExecutionContext(cliParser.parseString(commandLine))
        Future<?> commandFuture = commandExecutor.submit({ handleCommand(executionContext) } as Callable<Boolean>)
        Terminal terminal = console.reader.terminal
        if (terminal instanceof UnixTerminal) {
            ((UnixTerminal) terminal).disableInterruptCharacter()
        }
        try {
            while (!commandFuture.done) {
                if (nonBlockingInput.nonBlockingEnabled) {
                    int peeked = nonBlockingInput.peek(100L)
                    if (peeked > 0) {
                        // read peeked character from buffer
                        nonBlockingInput.read(1L)
                        if (peeked == KEYPRESS_CTRL_C || peeked == KEYPRESS_ESC) {
                            executionContext.console.log('  ')
                            executionContext.console.updateStatus('Stopping build. Please wait...')
                            executionContext.cancel()
                        }
                    }
                }
            }
        }
        finally {
            if (terminal instanceof UnixTerminal) {
                ((UnixTerminal) terminal).enableInterruptCharacter()
            }
        }
        if (!commandFuture.isCancelled()) {
            try {
                return commandFuture.get()
            }
            catch (ExecutionException e) {
                throw e.cause
            }
        }
        false
    }

    private initializeProfile() {
        BuildSettings.TARGET_DIR?.mkdirs()

        if (new File(BuildSettings.BASE_DIR, 'profile.yml').exists()) {
            this.profileRepository = createMavenProfileRepository()
        }
        else {
            populateContextLoader()
        }

        String profileName = applicationConfig.get(BuildSettings.PROFILE) ?: getSetting(BuildSettings.PROFILE, String, DEFAULT_PROFILE_NAME)
        this.profile = profileRepository.getProfile(profileName)

        if (profile == null) {
            throw new IllegalStateException("No profile found for name [$profileName].")
        }
    }

    protected void populateContextLoader() {
        try {
            if (new File(BuildSettings.BASE_DIR, 'build.gradle').exists()) {
                Map<String, List<URL>> dependencyMap = new MapReadingCachedGradleOperation<List<URL>>(projectContext, '.dependencies') {

                    @Override
                    void updateStatusMessage() {
                        GrailsConsole.instance.updateStatus('Resolving Dependencies. Please wait...')
                    }

                    @Override
                    List<URL> createMapValue(Object value) {
                        if (value !instanceof List) {
                            return []
                        }
                        ((List) value).collect { new URL(it.toString()) } as List<URL>
                    }

                    @Override
                    Map<String, List<URL>> readFromGradle(ProjectConnection connection) {
                        CodeGenConfig config = applicationConfig

                        BuildActionExecuter buildActionExecuter = connection.action(new ClasspathBuildAction())
                        buildActionExecuter.standardOutput = System.out
                        buildActionExecuter.standardError = System.err
                        buildActionExecuter.withArguments("-Dgrails.profile=${config.navigate('grails', 'profile')}")
                        buildActionExecuter.addProgressListener(new ProgressListener() {

                            @Override
                            void statusChanged(ProgressEvent event) {
                                GrailsConsole.instance.updateStatus(event.description)
                            }

                        })

                        GrailsClasspath grailsClasspath = buildActionExecuter.run()
                        if (grailsClasspath.error) {
                            GrailsConsole.instance.error("${grailsClasspath.error} Type 'gradle dependencies' for more information")
                            exit 1
                        }
                        [
                                dependencies: grailsClasspath.dependencies,
                                profiles: grailsClasspath.profileDependencies
                        ]
                    }

                }.call()

                List<URL> urls = (List<URL>) dependencyMap.get('dependencies') + (List<URL>) dependencyMap.get('profiles')
                List<URL> profiles = (List<URL>) dependencyMap.get('profiles')
                URLClassLoader classLoader = new URLClassLoader(urls as URL[], Thread.currentThread().contextClassLoader)
                this.profileRepository = new StaticJarProfileRepository(classLoader, profiles as URL[])
                Thread.currentThread().contextClassLoader = classLoader
            }
        }
        catch (Throwable e) {
            e = ExceptionUtils.getRootCause(e)
            GrailsConsole.instance.error("Error initializing classpath: $e.message", e)
            exit(1)
        }
    }

    private CodeGenConfig loadApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File(BuildSettings.GRAILS_APP_DIR, 'conf/application.yml')
        File applicationGroovy = new File(BuildSettings.GRAILS_APP_DIR, 'conf/application.groovy')
        if (applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        if (applicationGroovy.exists()) {
            config.loadGroovy(applicationGroovy)
        }
        config
    }

    private boolean handleBuiltInCommands(ExecutionContext context) {
        CommandLine commandLine = context.commandLine
        String commandName = commandLine.commandName

        if (commandName && commandName.size() > 1 && commandName.startsWith('!')) {
            return executeProcess(context, commandLine.rawArguments)
        }

        switch (commandName) {
            case '!':
                return bang(context)
            case 'exit':
                exitInteractiveMode()
                return true
            case 'quit':
                exitInteractiveMode()
                return true
            default:
                false
        }
    }

    protected boolean executeProcess(ExecutionContext context, String[] args) {
        GrailsConsole console = context.console
        try {
            args[0] = args[0].substring(1)
            Process process = new ProcessBuilder(args).redirectErrorStream(true).start()
            console.log process.inputStream.getText('UTF-8')
            return true
        }
        catch (e) {
            console.error "Error occurred executing process: $e.message"
            return false
        }
    }

    /**
     * Removes '\' escape characters from the given string.
     */
    private String unescape(String str) {
        str.replace('\\', '')
    }

    protected Boolean bang(ExecutionContext context) {
        GrailsConsole console = context.console
        History history = console.reader.history

        //move one step back to !
        history.previous()

        if (!history.previous()) {
            console.error '! not valid. Can not repeat without history'
        }

        //another step to previous command
        String historicalCommand = history.current()
        if (historicalCommand.startsWith('!')) {
            console.error "Can not repeat command: $historicalCommand"
        }
        else {
            return handleCommand(cliParser.parseString(historicalCommand))
        }
        false
    }

    private void exitInteractiveMode() {
        keepRunning = false
        try {
            GradleAsyncInvoker.POOL.shutdownNow()
        }
        catch (Throwable ignored) {
        }
    }

    static class ExecutionContextImpl implements ExecutionContext {

        CommandLine commandLine

        @Delegate(excludes = ['getConsole', 'getBaseDir'])
        ProjectContext projectContext

        GrailsConsole console = GrailsConsole.getInstance()

        ExecutionContextImpl(CodeGenConfig config) {
            this(new DefaultCommandLine(), new ProjectContextImpl(GrailsConsole.instance, new File('.'), config))
        }

        ExecutionContextImpl(CommandLine commandLine, ProjectContext projectContext) {
            this.commandLine = commandLine
            this.projectContext = projectContext
            if (projectContext?.console) {
                console = projectContext.console
            }
        }

        private final List<CommandCancellationListener> cancelListeners = []

        @Override
        void addCancelledListener(CommandCancellationListener listener) {
            cancelListeners << listener
        }

        @Override
        void cancel() {
            for (CommandCancellationListener listener : cancelListeners) {
                try {
                    listener.commandCancelled()
                }
                catch (Exception e) {
                    console.error('Error notifying listener about cancelling command', e)
                }
            }
        }

        @Override
        File getBaseDir() {
            this.projectContext?.baseDir ?: new File('.')
        }

    }

    @Canonical
    private static class ProjectContextImpl implements ProjectContext {

        GrailsConsole console = GrailsConsole.getInstance()
        File baseDir
        CodeGenConfig grailsConfig

        @Override
        String navigateConfig(String... path) {
            grailsConfig.navigate(path)
        }

        @Override
        ConfigMap getConfig() {
            grailsConfig
        }

        @Override
        <T> T navigateConfigForType(Class<T> requiredType, String... path) {
            grailsConfig.navigate(requiredType, path)
        }

    }

}
