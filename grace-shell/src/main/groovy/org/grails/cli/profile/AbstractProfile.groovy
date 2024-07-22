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
package org.grails.cli.profile

import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.transform.ToString
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

import grails.io.IOUtils
import grails.util.BuildSettings
import grails.util.CosineSimilarity

import org.grails.build.parsing.CommandLine
import org.grails.cli.interactive.completers.StringsCompleter
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.cli.profile.commands.DefaultMultiStepCommand
import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.config.NavigableMap
import org.grails.io.support.Resource

import static org.grails.cli.profile.ProfileUtil.createDependency

/**
 * Abstract implementation of the profile class
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
@ToString(includes = ['name'])
abstract class AbstractProfile implements Profile {

    protected final Resource profileDir
    protected String name
    protected List<Profile> parentProfiles
    protected Map<String, Command> commandsByName
    protected NavigableMap navigableConfig
    protected ProfileRepository profileRepository
    protected List<Dependency> dependencies = []
    protected List<String> repositories = []
    protected List<String> parentNames = []
    protected List<String> buildRepositories = []
    protected List<String> buildPlugins = []
    protected List<String> buildExcludes = []
    protected List<String> skeletonExcludes = []
    protected List<String> binaryExtensions = []
    protected List<String> executablePatterns = []
    protected final List<Command> internalCommands = []
    protected List<String> buildMerge = null
    protected List<Feature> features = []
    protected Set<String> defaultFeaturesNames = []
    protected Set<String> requiredFeatureNames = []
    protected String parentTargetFolder
    protected final ClassLoader classLoader
    protected ExclusionDependencySelector exclusionDependencySelector = new ExclusionDependencySelector()
    protected String description = ''
    protected String instructions = ''
    protected String version = BuildSettings.package.implementationVersion

    AbstractProfile(Resource profileDir) {
        this(profileDir, AbstractProfile.getClassLoader())
    }

    AbstractProfile(Resource profileDir, ClassLoader classLoader) {
        this.classLoader = classLoader
        this.profileDir = profileDir

        URL url = profileDir.getURL()
        File jarFile = IOUtils.findJarFile(url)
        Pattern pattern = ~/.+-(\d.+)\.jar/

        String path
        if (jarFile != null) {
            path = jarFile.name
        }
        else if (url != null) {
            String p = url.path
            path = p.substring(0, p.indexOf('.jar') + 4)
        }
        if (path) {
            Matcher matcher = pattern.matcher(path as CharSequence)
            if (matcher.matches()) {
                this.version = matcher.group(1)
            }
        }
    }

    @Override
    String getName() {
        name
    }

    String getVersion() {
        version
    }

    protected void initialize() {
        Resource profileYml = profileDir.createRelative('profile.yml')
        Map<String, Object> profileConfig = new Yaml(new SafeConstructor(new LoaderOptions())).<Map<String, Object>> load(profileYml.getInputStream())

        name = profileConfig.get('name')?.toString()
        description = profileConfig.get('description')?.toString() ?: ''
        instructions = profileConfig.get('instructions')?.toString() ?: ''

        Object parents = profileConfig.get('extends')
        if (parents) {
            parentNames = parents.toString().split(',')*.trim()
        }
        if (this.name == null) {
            throw new IllegalStateException("Profile name not set. Profile for path ${profileDir.URL} is invalid")
        }
        NavigableMap map = new NavigableMap()
        map.merge(profileConfig)
        navigableConfig = map
        Object commandsByName = profileConfig.get('commands')
        if (commandsByName instanceof Map) {
            Map commandsMap = (Map) commandsByName
            for (clsName in commandsMap.keySet()) {
                String fileName = commandsMap[clsName]
                if (fileName.endsWith('.groovy')) {
                    GroovyScriptCommand cmd = (GroovyScriptCommand) classLoader.loadClass(clsName.toString()).newInstance()
                    cmd.profile = this
                    cmd.profileRepository = profileRepository
                    internalCommands.add cmd
                }
                else if (fileName.endsWith('.yml')) {
                    Resource yamlCommand = profileDir.createRelative("commands/$fileName")
                    if (yamlCommand.exists()) {
                        Map<String, Object> data = new Yaml(new SafeConstructor(new LoaderOptions())).<Map>load(yamlCommand.getInputStream())
                        Command cmd = new DefaultMultiStepCommand(clsName.toString(), this, data)
                        Object minArguments = data?.minArguments
                        cmd.minArguments = minArguments instanceof Integer ? (Integer) minArguments : 1
                        internalCommands.add cmd
                    }
                }
            }
        }

        Object featuresConfig = profileConfig.get('features')
        if (featuresConfig instanceof Map) {
            Map featureMap = (Map) featuresConfig
            List<String> featureList = (List) featureMap.get('provided') ?: Collections.emptyList()
            List<String> defaultFeatures = (List) featureMap.get('defaults') ?: Collections.emptyList()
            List<String> requiredFeatures = (List) featureMap.get('required') ?: Collections.emptyList()
            for (fn in featureList) {
                Resource featureData = profileDir.createRelative("features/${fn}/feature.yml")
                if (featureData.exists()) {
                    DefaultFeature f = new DefaultFeature(this, fn.toString(), profileDir.createRelative("features/$fn/"))
                    features.add f
                }
            }

            defaultFeaturesNames.addAll(defaultFeatures)
            requiredFeatureNames.addAll(requiredFeatures)
        }

        Object dependenciesConfig = profileConfig.get('dependencies')

        if (dependenciesConfig instanceof List) {
            List<Exclusion> exclusions = []
            for (entry in dependenciesConfig) {
                if (entry instanceof Map) {
                    String scope = (String) entry.scope
                    String coords = (String) entry.coords
                    if (scope == 'excludes') {
                        DefaultArtifact artifact = new DefaultArtifact(coords)
                        exclusions.add(new Exclusion(artifact.groupId ?: null, artifact.artifactId ?: null,
                                artifact.classifier ?: null, artifact.extension ?: null))
                    }
                    else {
                        Dependency dependency = createDependency(coords, scope, entry)
                        dependencies.add(dependency)
                    }
                }
                exclusionDependencySelector = new ExclusionDependencySelector(exclusions)
            }
        }

        this.repositories = (List<String>) navigableConfig.get('repositories', [])

        this.buildRepositories = (List<String>) navigableConfig.get('build.repositories', [])
        this.buildPlugins = (List<String>) navigableConfig.get('build.plugins', [])
        this.buildExcludes = (List<String>) navigableConfig.get('build.excludes', [])
        this.buildMerge = (List<String>) navigableConfig.get('build.merge', null)
        this.parentTargetFolder = (String) navigableConfig.get('skeleton.parent.target', null)
        this.skeletonExcludes = (List<String>) navigableConfig.get('skeleton.excludes', [])
        this.binaryExtensions = (List<String>) navigableConfig.get('skeleton.binaryExtensions', [])
        this.executablePatterns = (List<String>) navigableConfig.get('skeleton.executable', [])
    }

    String getDescription() {
        description
    }

    String getInstructions() {
        instructions
    }

    Set<String> getBinaryExtensions() {
        Set<String> calculatedBinaryExtensions = []
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            calculatedBinaryExtensions.addAll(profile.binaryExtensions)
        }
        calculatedBinaryExtensions.addAll(binaryExtensions)
        calculatedBinaryExtensions
    }

    Set<String> getExecutablePatterns() {
        Set<String> calculatedExecutablePatterns = []
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            calculatedExecutablePatterns.addAll(profile.executablePatterns)
        }
        calculatedExecutablePatterns.addAll(executablePatterns)
        calculatedExecutablePatterns
    }

    @Override
    Iterable<Feature> getDefaultFeatures() {
        getFeatures().findAll { Feature f -> defaultFeaturesNames.contains(f.name) }
    }

    @Override
    Iterable<Feature> getRequiredFeatures() {
        Collection<Feature> requiredFeatureInstances = getFeatures().findAll { Feature f -> requiredFeatureNames.contains(f.name) }
        if (requiredFeatureInstances.size() != requiredFeatureNames.size()) {
            throw new IllegalStateException("One or more required features were not found on the classpath. Required features: $requiredFeatureNames")
        }
        requiredFeatureInstances
    }

    @Override
    Iterable<Feature> getFeatures() {
        Set<Feature> calculatedFeatures = []
        calculatedFeatures.addAll(features)
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            calculatedFeatures.addAll profile.features
        }
        calculatedFeatures
    }

    @Override
    List<String> getBuildMergeProfileNames() {
        if (buildMerge != null) {
            return this.buildMerge
        }

        List<String> mergeNames = []
        for (parent in getExtends()) {
            mergeNames.add(parent.name)
        }
        mergeNames.add(name)
        mergeNames
    }

    @Override
    List<String> getBuildRepositories() {
        List<String> calculatedRepositories = []
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            calculatedRepositories.addAll(profile.buildRepositories)
        }
        calculatedRepositories.addAll(buildRepositories)
        calculatedRepositories
    }

    @Override
    List<String> getBuildPlugins() {
        List<String> calculatedPlugins = []
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            List<String> dependencies = profile.buildPlugins
            for (dep in dependencies) {
                if (!buildExcludes.contains(dep)) {
                    calculatedPlugins.add(dep)
                }
            }
        }
        calculatedPlugins.addAll(buildPlugins)
        calculatedPlugins
    }

    @Override
    List<String> getRepositories() {
        List<String> calculatedRepositories = []
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            calculatedRepositories.addAll(profile.repositories)
        }
        calculatedRepositories.addAll(repositories)
        calculatedRepositories
    }

    List<Dependency> getDependencies() {
        List<Dependency> calculatedDependencies = []
        Iterable<Profile> parents = getExtends()
        for (profile in parents) {
            List<Dependency> dependencies = profile.dependencies
            for (dep in dependencies) {
                if (exclusionDependencySelector.selectDependency(dep)) {
                    calculatedDependencies.add(dep)
                }
            }
        }
        calculatedDependencies.addAll(dependencies)
        calculatedDependencies
    }

    ProfileRepository getProfileRepository() {
        profileRepository
    }

    void setProfileRepository(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository
    }

    Resource getProfileDir() {
        profileDir
    }

    @Override
    NavigableMap getConfiguration() {
        navigableConfig
    }

    @Override
    Resource getTemplate(String path) {
        profileDir.createRelative("templates/$path")
    }

    @Override
    Iterable<Profile> getExtends() {
        parentNames.collect { String name ->
            Profile parent = profileRepository.getProfile(name, true)
            if (parent == null) {
                throw new IllegalStateException("Profile [$name] declares an invalid dependency on parent profile [$name]")
            }
            parent
        }
    }

    @Override
    Iterable<Completer> getCompleters(ProjectContext context) {
        Iterable<Command> commands = getCommands(context)

        Collection<Completer> completers = []

        for (Command cmd in commands) {
            CommandDescription description = cmd.description
            StringsCompleter commandNameCompleter = new StringsCompleter(cmd.name)

            if (cmd instanceof Completer) {
                completers << new ArgumentCompleter(commandNameCompleter, (Completer) cmd)
            }
            else {
                if (description.completer) {
                    if (description.flags) {
                        completers << new ArgumentCompleter(
                                commandNameCompleter,
                                description.completer,
                                new StringsCompleter(description.flags.collect { CommandArgument arg -> "-$arg.name".toString() }))
                    }
                    else {
                        completers << new ArgumentCompleter(commandNameCompleter, description.completer)
                    }
                }
                else {
                    if (description.flags) {
                        completers << new ArgumentCompleter(
                                commandNameCompleter,
                                new StringsCompleter(description.flags.collect { CommandArgument arg -> "-$arg.name".toString() }))
                    }
                    else {
                        completers << commandNameCompleter
                    }
                }
            }
        }

        completers
    }

    @Override
    Command getCommand(ProjectContext context, String name) {
        getCommands(context)
        commandsByName[name]
    }

    @Override
    Iterable<Command> getCommands(ProjectContext context) {
        if (commandsByName == null) {
            commandsByName = new LinkedHashMap<String, Command>()
            List excludes = []
            Closure<AbstractProfile> registerCommand = { Command command ->
                String name = command.fullName
                if (!commandsByName.containsKey(name) && !excludes.contains(name)) {
                    if (command instanceof ProfileRepositoryAware) {
                        ((ProfileRepositoryAware) command).setProfileRepository(profileRepository)
                    }
                    commandsByName.put(name, command)
                    CommandDescription desc = command.description
                    Collection<String> synonyms = desc.synonyms
                    if (synonyms) {
                        for (String syn in synonyms) {
                            commandsByName.put(syn, command)
                        }
                    }
                    if (command instanceof ProjectContextAware) {
                        ((ProjectContextAware) command).projectContext = context
                    }
                    if (command instanceof ProfileCommand) {
                        ((ProfileCommand) command).profile = this
                    }
                }
            }

            CommandRegistry.findCommands(this).each(registerCommand)

            Iterable<Profile> parents = getExtends()
            if (parents) {
                excludes = (List) configuration.navigate('command', 'excludes') ?: []
                registerParentCommands(context, parents, registerCommand)
            }
        }
        commandsByName.values()
    }

    protected void registerParentCommands(ProjectContext context, Iterable<Profile> parents, Closure registerCommand) {
        for (parent in parents) {
            parent.getCommands(context).each registerCommand

            Iterable<Profile> extended = parent.extends
            if (extended) {
                registerParentCommands context, extended, registerCommand
            }
        }
    }

    @Override
    boolean hasCommand(ProjectContext context, String name) {
        getCommands(context) // ensure initialization
        commandsByName.containsKey(name)
    }

    @Override
    boolean handleCommand(ExecutionContext context) {
        getCommands(context) // ensure initialization

        CommandLine commandLine = context.commandLine
        String commandName = commandLine.commandName
        Command cmd = commandsByName[commandName]
        if (cmd) {
            Collection<CommandArgument> requiredArguments = cmd?.description?.arguments
            int requiredArgumentCount = requiredArguments?.findAll { CommandArgument ca -> ca.required }?.size() ?: 0
            if (commandLine.remainingArgs.size() < requiredArgumentCount) {
                context.console.error "Command [$commandName] missing required arguments: ${requiredArguments*.name}. " +
                        "Type 'grace help $commandName' for more info."
                return false
            }

            return cmd.handle(context)
        }

        context.console.error("Command not found ${context.commandLine.commandName}")
        List<String> mostSimilar = CosineSimilarity.mostSimilar(commandName, commandsByName.keySet())
        List<String> topMatches = mostSimilar.subList(0, Math.min(3, mostSimilar.size()))
        if (topMatches) {
            context.console.log("Did you mean: ${topMatches.join(' or ')}?")
        }
        false
    }

    @Override
    String getParentSkeletonDir() {
        this.parentTargetFolder
    }

    @Override
    File getParentSkeletonDir(File parent) {
        parentSkeletonDir ? new File(parent, parentSkeletonDir) : parent
    }

    List<String> getSkeletonExcludes() {
        this.skeletonExcludes
    }

}
