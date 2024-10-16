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
package org.grails.cli.profile.commands

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream

import groovy.ant.AntBuilder
import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.BuildLogger
import org.apache.tools.ant.MagicNames
import org.apache.tools.ant.Project
import org.apache.tools.ant.ProjectHelper
import org.apache.tools.ant.types.ResourceCollection
import org.apache.tools.ant.types.resources.FileResource
import org.apache.tools.ant.types.resources.URLResource
import org.eclipse.aether.graph.Dependency
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

import grails.build.logging.GrailsConsole
import grails.io.IOUtils
import grails.util.GrailsNameUtils
import grails.util.GrailsVersion
import org.grails.build.logging.GrailsConsoleAntBuilder
import org.grails.build.logging.GrailsConsoleAntProject
import org.grails.build.logging.GrailsConsoleLogger
import org.grails.build.parsing.CommandLine
import org.grails.cli.GrailsCli
import org.grails.cli.compiler.dependencies.GrailsDependenciesDependencyManagement
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Feature
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.commands.io.GradleDependency
import org.grails.cli.profile.repository.MavenProfileRepository
import org.grails.config.NavigableMap
import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource

import static org.grails.build.parsing.CommandLine.QUIET_ARGUMENT
import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Command for creating Grails applications
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class CreateAppCommand extends ArgumentCompletingCommand implements ProfileRepositoryAware {

    public static final String NAME = 'create-app'
    public static final String PROFILE_FLAG = 'profile'
    public static final String FEATURES_FLAG = 'features'
    public static final String TEMPLATE_FLAG = 'template'
    public static final String CSS_FLAG = 'css'
    public static final String JAVASCRIPT_FLAG = 'javascript'
    public static final String DATABASE_FLAG = 'database'
    public static final String ENABLE_PREVIEW_FLAG = 'enable-preview'
    public static final String ENCODING = System.getProperty('file.encoding') ?: 'UTF-8'
    public static final String INPLACE_FLAG = 'inplace'
    public static final String FORCE_FLAG = 'force'
    public static final String GRACE_VERSION_FLAG = 'grace-version'
    public static final String BOOT_VERSION_FLAG = 'boot-version'

    public static final String[] SUPPORT_GRACE_VERSIONS = ['2023', '2022', '6', '5', '4', '3']
    public static final String[] SUPPORT_SPRING_BOOT_VERSIONS = ['3.1', '3.2', '3.3', '3.4']

    public static final String UNZIP_PROFILE_TEMP_DIR = 'grails-profile-'
    public static final String UNZIP_TEMPLATE_TEMP_DIR = 'grails-template-'

    protected static final String APPLICATION_YML = 'application.yml'
    protected static final String BUILD_GRADLE = 'build.gradle'
    protected static final String GRADLE_PROPERTIES = 'gradle.properties'

    ProfileRepository profileRepository

    CommandDescription description = new CommandDescription(name, 'Creates an application', 'create-app [NAME] --profile=web')

    CreateAppCommand() {
        populateDescription()
        description.flag(name: INPLACE_FLAG, description: 'Used to create an application using the current directory')
        description.flag(name: PROFILE_FLAG, description: 'The profile to use', required: false)
        description.flag(name: FEATURES_FLAG, description: 'The features to use', required: false)
        description.flag(name: TEMPLATE_FLAG, description: 'The application template to use', required: false)
        description.flag(name: CSS_FLAG, description: 'The CSS framework to use', required: false)
        description.flag(name: JAVASCRIPT_FLAG, description: 'The JavaScript approach', required: false)
        description.flag(name: DATABASE_FLAG, description: 'The Database type', required: false)
        description.flag(name: STACKTRACE_ARGUMENT, description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, description: 'Show verbose output', required: false)
        description.flag(name: QUIET_ARGUMENT, description: 'Suppress status output', required: false)
        description.flag(name: ENABLE_PREVIEW_FLAG, description: 'Enable preview features', required: false)
        description.flag(name: GRACE_VERSION_FLAG, description: 'Specific Grace Version', required: false)
        description.flag(name: BOOT_VERSION_FLAG, description: 'Specific Spring Boot Version', required: false)
        description.flag(name: FORCE_FLAG, description: 'Force overwrite of existing files', required: false)
    }

    protected void populateDescription() {
        description.argument(name: 'Application Name', description: 'The name of the application to create.', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    protected String getDefaultProfile() {
        ProfileRepository.DEFAULT_PROFILE_NAME
    }

    @Override
    protected int complete(CommandLine commandLine, CommandDescription desc, List<CharSequence> candidates, int cursor) {
        Map.Entry<String, Object> lastOption = commandLine.lastOption()
        if (lastOption != null) {
            // if value == true it means no profile is specified and only the flag is present
            List<String> profileNames = this.profileRepository.allProfiles*.name
            if (lastOption.key == PROFILE_FLAG) {
                def val = lastOption.value
                if (val == true) {
                    candidates.addAll(profileNames)
                    return cursor
                }
                else if (!profileNames.contains(val)) {
                    String valStr = val.toString()

                    List<String> candidateProfiles = profileNames.findAll { String pn ->
                        pn.startsWith(valStr)
                    }.collect { String pn ->
                        "${pn.substring(valStr.size())} ".toString()
                    }
                    candidates.addAll candidateProfiles
                    return cursor
                }
            }
            else if (lastOption.key == FEATURES_FLAG) {
                Object val = lastOption.value
                Profile profile = this.profileRepository.getProfile(commandLine.hasOption(PROFILE_FLAG) ?
                        commandLine.optionValue(PROFILE_FLAG).toString() : getDefaultProfile())
                List<String> featureNames = profile.features*.name
                if (val == true) {
                    candidates.addAll(featureNames)
                    return cursor
                }
                else if (!profileNames.contains(val)) {
                    String valStr = val.toString()
                    if (valStr.endsWith(',')) {
                        String[] specified = valStr.split(',')
                        candidates.addAll(featureNames.findAll { String f ->
                            !specified.contains(f)
                        })
                        return cursor
                    }

                    List<String> candidatesFeatures = featureNames.findAll { String pn ->
                        pn.startsWith(valStr)
                    }.collect { String pn ->
                        "${pn.substring(valStr.size())} ".toString()
                    }
                    candidates.addAll candidatesFeatures
                    return cursor
                }
            }
        }
        super.complete(commandLine, desc, candidates, cursor)
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine

        String profileName = commandLine.optionValue('profile')?.toString() ?: getDefaultProfile()

        List<String> validFlags = [INPLACE_FLAG, PROFILE_FLAG, FEATURES_FLAG, TEMPLATE_FLAG,
                                   CSS_FLAG, JAVASCRIPT_FLAG, DATABASE_FLAG,
                                   STACKTRACE_ARGUMENT, VERBOSE_ARGUMENT, QUIET_ARGUMENT,
                                   GRACE_VERSION_FLAG, BOOT_VERSION_FLAG, FORCE_FLAG]
        if (!commandLine.hasOption(ENABLE_PREVIEW_FLAG)) {
            commandLine.undeclaredOptions.each { String key, Object value ->
                if (!validFlags.contains(key)) {
                    List possibleSolutions = validFlags.findAll { it.substring(0, 2) == key.substring(0, 2) }
                    StringBuilder warning = new StringBuilder("Unrecognized flag: ${key}.")
                    if (possibleSolutions) {
                        warning.append(' Possible solutions: ')
                        warning.append(possibleSolutions.join(', '))
                    }
                    console.warn(warning.toString())
                }
            }
        }

        String springBootVersion = new GrailsDependenciesDependencyManagement().springBootVersion
        String grailsVersion = GrailsVersion.current().version
        String specificGraceVersion = commandLine.optionValue(GRACE_VERSION_FLAG)
        String specificBootVersion = commandLine.optionValue(BOOT_VERSION_FLAG)
        boolean inPlace = commandLine.hasOption('inplace') || GrailsCli.isInteractiveModeActive()
        String appName = commandLine.remainingArgs ? commandLine.remainingArgs[0] : ''

        List<String> features = commandLine.optionValue('features')?.toString()?.split(',')?.toList()
        Map<String, String> args = getCommandArguments(commandLine)

        CreateAppCommandObject cmd = new CreateAppCommandObject(
                appName: appName,
                baseDir: executionContext.baseDir,
                profileName: profileName,
                grailsVersion: specificGraceVersion ?: grailsVersion,
                springBootVersion: specificBootVersion ?: springBootVersion,
                features: features,
                template: commandLine.optionValue('template'),
                inplace: inPlace,
                stacktrace: commandLine.hasOption(STACKTRACE_ARGUMENT),
                verbose: commandLine.hasOption(VERBOSE_ARGUMENT),
                quiet: commandLine.hasOption(QUIET_ARGUMENT),
                force: commandLine.hasOption(FORCE_FLAG),
                console: console,
                args: args
        )

        if (commandLine.hasOption(GRACE_VERSION_FLAG) && specificGraceVersion != grailsVersion) {
            if (validateSpecificGraceVersion(specificGraceVersion)) {
                this.profileRepository = new MavenProfileRepository(specificGraceVersion)
            }
            else {
                console.error("Specific Grace version `$specificGraceVersion` is not supported!")
                return false
            }
        }
        this.handle(cmd)
    }

    boolean handle(CreateAppCommandObject cmd) {
        if (this.profileRepository == null) {
            throw new IllegalStateException("Property 'profileRepository' must be set")
        }

        String grailsVersion = cmd.grailsVersion
        GrailsConsole console = cmd.console
        String profileName = cmd.profileName

        Profile profileInstance = this.profileRepository.getProfile(profileName)
        if (!validateProfile(profileInstance, profileName, console)) {
            return false
        }

        if (!cmd.appName && !cmd.inplace) {
            console.error('Specify an application name or use --inplace to create an application in the current directory')
            return false
        }

        String appName
        String groupName
        String defaultPackageName
        String groupAndAppName = cmd.appName
        if (cmd.inplace) {
            appName = new File('.').canonicalFile.name
            groupAndAppName = groupAndAppName ?: appName
        }

        if (!groupAndAppName) {
            console.error('Specify an application name or use --inplace to create an application in the current directory')
            return false
        }

        try {
            List<String> parts = groupAndAppName.split(/\./) as List
            if (parts.size() == 1) {
                appName = parts[0]
                defaultPackageName = appName.split(/[-]+/).collect { String token ->
                    (token.toLowerCase().toCharArray().findAll { char ch ->
                        Character.isJavaIdentifierPart(ch)
                    } as char[]) as String
                }.join('.')

                if (!GrailsNameUtils.isValidJavaPackage(defaultPackageName)) {
                    console.error("Cannot create a valid package name for [$appName]. " +
                            'Please specify a name that is also a valid Java package.')
                    return false
                }
                groupName = defaultPackageName
            }
            else {
                appName = parts[-1]
                groupName = parts[0..-2].join('.')
                defaultPackageName = groupName
            }
        }
        catch (IllegalArgumentException e) {
            console.error(e.message)
            return false
        }

        Path appFullDirectory = Paths.get(cmd.baseDir.path, appName)
        File projectTargetDirectory = cmd.inplace ? new File('.').canonicalFile : appFullDirectory.toAbsolutePath().normalize().toFile()
        if (projectTargetDirectory.exists() && !isDirectoryEmpty(projectTargetDirectory)) {
            if (cmd.force) {
                cleanDirectory(projectTargetDirectory)
            }
            else {
                console.error("Directory `${projectTargetDirectory.absolutePath}` already exists!" +
                        ' Use --force if you want to overwrite or specify an alternate location.')
                return false
            }
        }
        else {
            boolean result = projectTargetDirectory.mkdir()
            if (!result) {
                console.error("Directory `${projectTargetDirectory.absolutePath}` created faild!")
            }
        }

        List<Feature> features = evaluateFeatures(profileInstance, cmd.features, cmd.console)

        Map<String, String> variables = initializeVariables(appName, groupName, defaultPackageName, profileName, features, cmd.template, cmd.grailsVersion)
        Map<String, String> args = new HashMap<>()
        args.putAll(cmd.args)
        args.put(FEATURES_FLAG, features*.name?.join(','))

        Project project = createAntProject(cmd.appName, projectTargetDirectory, variables, args, console, cmd.verbose, cmd.quiet)
        GrailsConsoleAntBuilder ant = new GrailsConsoleAntBuilder(project)

        String projectType = getName().substring(7)

        console.addStatus("Creating a new ${projectType == 'app' ? 'application' : projectType}")
        console.println()
        console.println("     Name:".padRight(20) + appName)
        console.println("     Package:".padRight(20) + defaultPackageName)
        console.println("     Profile:".padRight(20) + profileName)
        if (features) {
            console.println("     Features:".padRight(20) + features*.name?.join(', '))
        }
        if (cmd.template) {
            console.println("     Template:".padRight(20) + cmd.template)
        }
        console.println("     Project root:".padRight(20) + projectTargetDirectory.absolutePath)
        console.println()

        generateProjectSkeleton(ant, profileInstance, features, variables, projectTargetDirectory, cmd.verbose, cmd.quiet)

        if (cmd.template) {
            if (cmd.template.endsWith('.groovy')) {
                replaceBuildTokens(ant, profileName, profileInstance, features, variables, grailsVersion, projectTargetDirectory)
                applyApplicationTemplate(ant, console, cmd.appName, cmd.template, projectTargetDirectory, cmd.verbose, cmd.quiet)
            }
            else if (cmd.template.endsWith('.zip') || cmd.template.endsWith('.git') || new File(cmd.template).isDirectory()) {
                copyApplicationTemplate(ant, console, appName, groupName, defaultPackageName, profileInstance,
                        features, cmd.template, grailsVersion, variables, args, projectTargetDirectory, cmd.verbose, cmd.quiet)
            }
        }
        else {
            replaceBuildTokens(ant, profileName, profileInstance, features, variables, grailsVersion, projectTargetDirectory)
        }

        if (cmd.springBootVersion) {
            updateSpringDependencies(ant, grailsVersion, cmd.springBootVersion, projectTargetDirectory)
        }

        String result = String.format("%s created by %s %s.", projectType == 'app' ? 'Application' : projectType.capitalize(),
                GrailsVersion.isGrace(grailsVersion) ? 'Grace' : 'Grails', grailsVersion)

        if (cmd.quiet) {
            console.updateStatus(result)
        }
        else {
            console.addStatus(result)
        }

        if (profileInstance.instructions) {
            console.addStatus(profileInstance.instructions)
        }

        GrailsCli.triggerAppLoad()
        true
    }

    protected boolean validateProfile(Profile profileInstance, String profileName, GrailsConsole console) {
        if (profileInstance == null) {
            console.error("Profile not found for name [$profileName]")
            return false
        }
        true
    }

    protected List<Feature> evaluateFeatures(Profile profile, List<String> requestedFeatures, GrailsConsole console) {
        List<Feature> features
        if (requestedFeatures) {
            List<String> allFeatureNames = profile.features*.name
            Collection<String> validFeatureNames = requestedFeatures.intersect(allFeatureNames)
            requestedFeatures.removeAll(allFeatureNames)
            requestedFeatures.each { String invalidFeature ->
                List<String> possibleSolutions = allFeatureNames.findAll {
                    it.substring(0, 2) == invalidFeature.substring(0, 2)
                }
                StringBuilder warning = new StringBuilder("Feature ${invalidFeature} does not exist in the profile ${profile.name}!")
                if (possibleSolutions) {
                    warning.append(' Possible solutions: ')
                    warning.append(possibleSolutions.join(', '))
                }
                console.warn(warning.toString())
            }
            features = (profile.features.findAll { Feature f -> validFeatureNames.contains(f.name) } + profile.requiredFeatures).unique()
        }
        else {
            features = (profile.defaultFeatures + profile.requiredFeatures).toList().unique()
        }
        features?.sort {
            it.name
        }
    }

    protected void generateProjectSkeleton(GrailsConsoleAntBuilder ant, Profile profileInstance,
                                           List<Feature> features, Map<String, String> variables,
                                           File projectTargetDirectory, boolean verbose, boolean quiet = false) {
        List<Profile> profiles = this.profileRepository.getProfileAndDependencies(profileInstance)

        final Map<URL, File> unzippedDirectories = new LinkedHashMap<URL, File>()
        Map<Profile, File> targetDirs = [:]
        buildTargetFolders(profileInstance, targetDirs, projectTargetDirectory)

        for (Profile p : profiles) {
            Set<File> ymlFiles = findAllFilesByName(projectTargetDirectory, APPLICATION_YML)
            Map<File, String> ymlCache = [:]

            File targetDirectory = targetDirs[p]

            ymlFiles.each { File applicationYmlFile ->
                String previousApplicationYml = (applicationYmlFile.isFile()) ? applicationYmlFile.getText(ENCODING) : null
                if (previousApplicationYml) {
                    ymlCache[applicationYmlFile] = previousApplicationYml
                }
            }

            copySkeleton(ant, profileInstance, p, variables, targetDirectory, unzippedDirectories)

            ymlCache.each { File applicationYmlFile, String previousApplicationYml ->
                if (applicationYmlFile.exists()) {
                    appendToYmlSubDocument(applicationYmlFile, previousApplicationYml)
                }
            }
        }

        for (Feature f in features) {
            Resource location = f.location

            File skeletonDir
            File tmpDir
            if (location instanceof FileSystemResource) {
                skeletonDir = location.createRelative('skeleton').file
            }
            else {
                tmpDir = unzipProfile(ant, unzippedDirectories, location)
                skeletonDir = new File(tmpDir, "META-INF/grails-profile/features/$f.name/skeleton")
            }

            File targetDirectory = targetDirs[f.profile]

            appendFeatureFiles(skeletonDir, targetDirectory)

            if (skeletonDir.exists()) {
                copySrcToTarget(ant, skeletonDir, ['**/' + APPLICATION_YML], profileInstance.binaryExtensions, variables, targetDirectory)
            }
        }

        // Cleanup temporal directories
        unzippedDirectories.values().each { File tmpDir ->
            deleteDirectoryOrFile(tmpDir)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void copySkeleton(GrailsConsoleAntBuilder ant, Profile profile, Profile participatingProfile,
                                Map<String, String> variables, File targetDirectory, Map<URL, File> unzippedDirectories) {
        List<String> buildMergeProfileNames = profile.buildMergeProfileNames
        List<String> excludes = profile.skeletonExcludes
        if (profile == participatingProfile) {
            excludes = []
        }

        Resource skeletonResource = participatingProfile.profileDir.createRelative('skeleton')
        File skeletonDir
        File tmpDir
        if (skeletonResource instanceof FileSystemResource) {
            skeletonDir = skeletonResource.file
        }
        else {
            // establish the JAR file name and extract
            tmpDir = unzipProfile(ant, unzippedDirectories, skeletonResource)
            skeletonDir = new File(tmpDir, 'META-INF/grails-profile/skeleton')
        }
        copySrcToTarget(ant, skeletonDir, excludes, profile.binaryExtensions, variables, targetDirectory)

        Set<File> sourceBuildGradles = findAllFilesByName(skeletonDir, BUILD_GRADLE)

        sourceBuildGradles.each { File srcFile ->
            File srcDir = srcFile.parentFile
            File destDir = getDestinationDirectory(srcFile, targetDirectory)
            File destFile = new File(destDir, BUILD_GRADLE)

            if (new File(srcDir, '.gitattributes').exists()) {
                ant.copy(file: "${srcDir}/.gitattributes", todir: destDir, failonerror: false)
            }
            if (new File(srcDir, '.gitignore').exists()) {
                ant.copy(file: "${srcDir}/.gitignore", todir: destDir, failonerror: false)
            }

            if (!destFile.exists()) {
                ant.copy file: srcFile, tofile: destFile
            }
            else if (buildMergeProfileNames.contains(participatingProfile.name)) {
                def concatFile = "${destDir}/concat-build.gradle"
                ant.move(file: destFile, tofile: concatFile)
                ant.concat([destfile: destFile, fixlastline: true], {
                    path {
                        pathelement location: concatFile
                        pathelement location: srcFile
                    }
                })
                ant.delete(file: concatFile, failonerror: false)
            }
        }

        Set<File> sourceGradleProperties = findAllFilesByName(skeletonDir, GRADLE_PROPERTIES)

        sourceGradleProperties.each { File srcFile ->
            File destDir = getDestinationDirectory(srcFile, targetDirectory)
            File destFile = new File(destDir, GRADLE_PROPERTIES)

            if (destFile.exists()) {
                def concatGradlePropertiesFile = "${destDir}/concat-gradle.properties"
                ant.move(file: destFile, tofile: concatGradlePropertiesFile)
                ant.concat([destfile: destFile, fixlastline: true], {
                    path {
                        pathelement location: concatGradlePropertiesFile
                        pathelement location: srcFile
                    }
                })
                ant.delete(file: concatGradlePropertiesFile, failonerror: false)
            }
            else {
                ant.copy file: srcFile, tofile: destFile
            }
        }

        ant.chmod(dir: targetDirectory, includes: profile.executablePatterns.join(' '), perm: 'u+x')
    }

    @CompileDynamic
    protected void copySrcToTarget(GrailsConsoleAntBuilder ant, File srcDir, List excludes, Set<String> binaryFileExtensions,
                                   Map<String, String> variables, File targetDirectory) {
        ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
            fileSet(dir: srcDir, casesensitive: false) {
                for (exc in excludes) {
                    exclude name: exc
                }
                exclude name: '**/' + BUILD_GRADLE
                exclude name: '**/' + GRADLE_PROPERTIES
                binaryFileExtensions.each { ext ->
                    exclude(name: "**/*.${ext}")
                }
            }
            filterset {
                variables.each { k, v ->
                    filter(token: k, value: v)
                }
            }
            mapper {
                filtermapper {
                    variables.each { k, v ->
                        replacestring(from: "@${k}@".toString(), to: v)
                    }
                }
            }
        }
        ant.copy(todir: targetDirectory, overwrite: true) {
            fileSet(dir: srcDir, casesensitive: false) {
                binaryFileExtensions.each { ext ->
                    include(name: "**/*.${ext}")
                }
                for (exc in excludes) {
                    exclude name: exc
                }
                exclude name: '**/' + BUILD_GRADLE
            }
        }
    }

    @CompileDynamic
    protected void copyApplicationTemplate(GrailsConsoleAntBuilder ant, GrailsConsole console,
                                           String appName, String groupName, String packageName, Profile profile,
                                           List<Feature> features, String templateUrl, String grailsVersion,
                                           Map<String, String> variables, Map<String, String> args, File targetDirectory,
                                           boolean verbose, boolean quiet = false) {
        if (quiet) {
            console.updateStatus('Applying Template')
        }
        else {
            console.addStatus('Applying Template')
            console.println()
        }

        // Define Ant tasks
        ant.taskdef(resource: 'org/grails/cli/profile/tasks/antlib.xml')

        File tempZipFile = null
        File tempDir = null
        File projectDir = null
        try {
            if (templateUrl.endsWith('.zip')) {
                if (!quiet) {
                    console.println("    [unzip] src: " + templateUrl)
                }
                tempZipFile = Files.createTempFile(UNZIP_TEMPLATE_TEMP_DIR, '.zip').toFile()
                ant.get(src: templateUrl, dest: tempZipFile)

                tempDir = Files.createTempDirectory(UNZIP_TEMPLATE_TEMP_DIR).toFile()
                ant.unzip(src: tempZipFile, dest: tempDir)
                if (quiet) {
                    console.updateStatus('Unzip template: ' + templateUrl)
                }
                else {
                    console.println()
                }

                Files.walkFileTree(tempDir.absoluteFile.toPath(), new SimpleFileVisitor<Path>() {

                    @Override
                    FileVisitResult visitFile(Path path, BasicFileAttributes attributes)
                            throws IOException {
                        if (path.fileName.toString() == 'project.yml') {
                            projectDir = path.parent.toFile()

                            FileVisitResult.TERMINATE
                        } else {
                            FileVisitResult.CONTINUE
                        }
                    }

                })
            } else if (templateUrl.endsWith('.git')) {
                if (!quiet) {
                    console.println("      [git] clone: " + templateUrl)
                }
                tempDir = Files.createTempDirectory(UNZIP_TEMPLATE_TEMP_DIR).toFile()
                ant.exec(executable: 'git') {
                    arg value: 'clone'
                    arg value: templateUrl
                    arg value: tempDir
                }
                if (quiet) {
                    console.updateStatus('Clone repository: ' + templateUrl)
                }
                else {
                    console.println()
                }

                Files.walkFileTree(tempDir.absoluteFile.toPath(), new SimpleFileVisitor<Path>() {

                    @Override
                    FileVisitResult visitFile(Path path, BasicFileAttributes attributes)
                            throws IOException {
                        if (path.fileName.toString() == 'project.yml') {
                            projectDir = path.parent.toFile()

                            FileVisitResult.TERMINATE
                        } else {
                            FileVisitResult.CONTINUE
                        }
                    }

                })
            }
            else {
                tempDir = Files.createTempDirectory(UNZIP_TEMPLATE_TEMP_DIR).toFile()
                ant.copy(todir: tempDir) {
                    fileSet(dir: templateUrl)
                }
                projectDir = tempDir
            }

            if (projectDir == null || !projectDir.isDirectory() || !projectDir.exists()) {
                console.error("`${templateUrl}` is not a valid template!")
                console.println()
                return
            }

            File projectYml = new File(projectDir, 'project.yml')
            File templateDir = new File(projectDir, 'template')

            if (!projectYml.exists() || !templateDir.exists() || !templateDir.isDirectory()) {
                console.error("`${templateUrl}` is not a valid template!")
                console.println()
                return
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()))
            Map<String, Object> templateConfig = yaml.<Map<String, Object>> load(new FileInputStream(projectYml))
            NavigableMap navigableConfig = new NavigableMap()
            navigableConfig.merge(templateConfig)

            String preApplyTemplateFile = navigableConfig.get('scripts.preApplyTemplate', 'scripts/pre_apply_template.groovy')
            String postApplyTemplateFile = navigableConfig.get('scripts.postApplyTemplate', 'scripts/post_apply_template.groovy')
            File preApplyTemplateScript = new File(projectDir, preApplyTemplateFile)
            File postApplyTemplateScript = new File(projectDir, postApplyTemplateFile)

            if (!verbose && !quiet) {
                ant.setLoggerLevel(Project.MSG_INFO)
            }
            if (preApplyTemplateScript.exists()) {
                ant.groovy(src: preApplyTemplateScript)
                if (quiet) {
                    console.updateStatus('Apply template: ' + preApplyTemplateScript.canonicalPath)
                }
                else {
                    console.println()
                }
            }
            if (!verbose || quiet) {
                ant.setLoggerLevel(Project.MSG_ERR)
            }
            Map<String, Object> binding = getApplicationTemplateBinding(appName, groupName, packageName, profile, features, args, grailsVersion, templateUrl)
            Set<File> groovyTemplateFiles = findAllFilesByName(templateDir, '.tpl')
            groovyTemplateFiles.each { File srcFile ->
                File destFile = new File(srcFile.parentFile, srcFile.name - '.tpl')
                TemplateEngine templateEngine = new GStringTemplateEngine()
                Template template = templateEngine.createTemplate(srcFile)
                destFile.withWriter('UTF-8') { Writer w ->
                    template.make(binding).writeTo(w)
                    w.flush()
                }
            }

            List<String> excludes = ['**/*.tpl']
            Set<String> binaryFileExtensions = (profile.binaryExtensions + (Set<String>) templateConfig.getOrDefault('template.binaryExtensions', [])).unique()
            Set<String> executablePatterns = (profile.executablePatterns + (Set<String>) templateConfig.getOrDefault('template.executablePatterns', [])).unique()

            ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
                fileSet(dir: templateDir, casesensitive: false) {
                    for (exc in excludes) {
                        exclude name: exc
                    }
                    binaryFileExtensions.each { ext ->
                        exclude(name: "**/*.${ext}")
                    }
                }
                filterset {
                    variables.each { k, v ->
                        filter(token: k, value: v)
                    }
                }
                mapper {
                    filtermapper {
                        variables.each { k, v ->
                            replacestring(from: "@${k}@".toString(), to: v)
                        }
                    }
                }
            }
            ant.copy(todir: targetDirectory, overwrite: true) {
                fileSet(dir: templateDir, casesensitive: false) {
                    binaryFileExtensions.each { ext ->
                        include(name: "**/*.${ext}")
                    }
                    for (exc in excludes) {
                        exclude name: exc
                    }
                }
            }

            ant.chmod(dir: targetDirectory, includes: executablePatterns.join(' '), perm: 'u+x')

            if (!verbose && !quiet) {
                ant.setLoggerLevel(Project.MSG_INFO)
            }
            if (postApplyTemplateScript.exists()) {
                ant.groovy(src: postApplyTemplateScript)
                if (quiet) {
                    console.updateStatus('Apply template: ' + postApplyTemplateScript.canonicalPath)
                }
                else {
                    console.println()
                }
            }
            if (!verbose || quiet) {
                ant.setLoggerLevel(Project.MSG_ERR)
            }

            replaceBuildTokens(ant, profile.name, profile, features, variables, grailsVersion, targetDirectory)

            String postGenerateProjectFile = navigableConfig.get('scripts.postGenerateProject', 'scripts/post_generate_project.groovy')
            File postGenerateProjectScript = new File(projectDir, postGenerateProjectFile)

            if (!verbose && !quiet) {
                ant.setLoggerLevel(Project.MSG_INFO)
            }
            if (postGenerateProjectScript.exists()) {
                ant.groovy(src: postGenerateProjectScript)
                if (quiet) {
                    console.updateStatus('Apply template: ' + postGenerateProjectScript.canonicalPath)
                }
                else {
                    console.println()
                }
            }
            if (!verbose || quiet) {
                ant.setLoggerLevel(Project.MSG_ERR)
            }
        }
        catch (Exception ex) {
            console.error("Can not apply template `${templateUrl}`!", ex)
        }
        finally {
            // Cleanup temporal directories
            deleteDirectoryOrFile(tempZipFile)
            deleteDirectoryOrFile(tempDir)
        }
    }

    @CompileDynamic
    protected void applyApplicationTemplate(GrailsConsoleAntBuilder ant, GrailsConsole console,
                                            String appName, String template,
                                            File projectTargetDirectory, boolean verbose, boolean quiet = false) {
        if (!quiet) {
            console.addStatus('Applying Template')
            console.println()
        }

        ant.taskdef(resource: 'org/grails/cli/profile/tasks/antlib.xml')

        if (!verbose && !quiet) {
            ant.setLoggerLevel(Project.MSG_INFO)
        }
        ResourceCollection resource
        if (template.startsWith('http://') || template.startsWith('https://') || template.startsWith('file://')) {
            resource = new URLResource(template)
            if (!resource.isExists()) {
                console.error("Template resource `${template}` is not exists!\n")
                return
            }
            if (quiet) {
                console.updateStatus('Apply template: ' + template)
            }
            ant.groovy {
                url url: template
            }
        }
        else {
            File file = new File(template)
            resource = new FileResource(file)
            if (!resource.isExists()) {
                console.error("Template resource `${template}` is not exists!\n")
                return
            }
            if (quiet) {
                console.updateStatus('Apply template: ' + template)
            }
            ant.groovy(src: template)
        }
        if (!verbose || quiet) {
            ant.setLoggerLevel(Project.MSG_ERR)
        }
        if (!quiet) {
            console.println()
        }
    }

    @CompileDynamic
    protected void replaceBuildTokens(GrailsConsoleAntBuilder ant, String profileName, Profile profile,
                                      List<Feature> features, Map<String, String> variables, String grailsVersion, File targetDirectory) {
        boolean isSnapshotVersion = GrailsVersion.current().isSnapshot()
        String ln = System.getProperty('line.separator')

        Closure repositoryUrl = { int spaces, String repo ->
            repo.startsWith('http') ? "${' ' * spaces}maven { url \"${repo}\" }" : "${' ' * spaces}${repo}"
        }
        List<String> repositoryUrls = profile.repositories.sort().reverse()
        if (GrailsVersion.isGraceSnapshotVersion(grailsVersion)) {
            repositoryUrls.add(0, 'maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }')
        }
        if (isSnapshotVersion) {
            repositoryUrls.add(0, 'mavenLocal()')
        }
        String repositories = repositoryUrls.collect(repositoryUrl.curry(4)).unique().join(ln)

        List<Dependency> profileDependencies = profile.dependencies
        List<Dependency> dependencies = profileDependencies.findAll { Dependency dep ->
            dep.scope != 'build'
        }
        List<Dependency> buildDependencies = profileDependencies.findAll { Dependency dep ->
            dep.scope == 'build'
        }

        for (Feature f in features) {
            dependencies.addAll f.dependencies.findAll { Dependency dep -> dep.scope != 'build' }
            buildDependencies.addAll f.dependencies.findAll { Dependency dep -> dep.scope == 'build' }
        }

        dependencies.add(new Dependency(this.profileRepository.getProfileArtifact(profileName), 'profile'))

        dependencies = dependencies.unique()

        List<GradleDependency> gradleDependencies = convertToGradleDependencies(dependencies, grailsVersion)

        String dependenciesString = gradleDependencies
                .sort({ GradleDependency dep -> dep.scope })
                .collect({ GradleDependency dep -> dep.toString(4) })
                .unique()
                .join(ln)

        List<String> buildRepositories = profile.buildRepositories
        for (Feature f in features) {
            buildRepositories.addAll(f.getBuildRepositories())
        }

        List<String> buildRepositoryUrls = buildRepositories.sort().reverse()

        if (GrailsVersion.isGraceSnapshotVersion(grailsVersion)) {
            buildRepositoryUrls.add(0, 'maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }')
        }
        if (isSnapshotVersion) {
            buildRepositoryUrls.add(0, 'mavenLocal()')
        }
        String buildRepositoriesString = GrailsVersion.isGrace2023(grailsVersion) ?
                buildRepositoryUrls.collect(repositoryUrl.curry(4)).unique().join(ln) :
                buildRepositoryUrls.collect(repositoryUrl.curry(8)).unique().join(ln)

        String buildDependenciesString = buildDependencies.collect { Dependency dep ->
            GrailsVersion.isGrace2023(grailsVersion) ?
                    "    implementation \"${dep.artifact.groupId}:${dep.artifact.artifactId}:${dep.artifact.version}\"" :
                    "        classpath \"${dep.artifact.groupId}:${dep.artifact.artifactId}:${dep.artifact.version}\""
        }.unique().join(ln)

        List<GString> buildPlugins = profile.buildPlugins.collect { String name ->
            GrailsVersion.isGrace2023(grailsVersion) ?
                    "    id \"$name\"" :
                    "apply plugin: \"$name\""
        }

        for (Feature f in features) {
            buildPlugins.addAll f.buildPlugins.collect { String name ->
                GrailsVersion.isGrace2023(grailsVersion) ?
                        "    id \"$name\"" :
                        "apply plugin: \"$name\""
            }
        }

        String buildPluginsString = buildPlugins.unique().join(ln)

        ant.replace(dir: targetDirectory) {
            replacefilter {
                replacetoken('@buildPlugins@')
                replacevalue(buildPluginsString)
            }
            replacefilter {
                replacetoken('@dependencies@')
                replacevalue(dependenciesString)
            }
            replacefilter {
                replacetoken('@buildDependencies@')
                replacevalue(buildDependenciesString)
            }
            replacefilter {
                replacetoken('@buildRepositories@')
                replacevalue(buildRepositoriesString)
            }
            replacefilter {
                replacetoken('@repositories@')
                replacevalue(repositories)
            }
            variables.each { String k, String v ->
                replacefilter {
                    replacetoken("@${k}@".toString())
                    replacevalue(v)
                }
            }
        }
    }

    @CompileDynamic
    protected void updateSpringDependencies(GrailsConsoleAntBuilder ant, String grailsVersion, String springBootVersion, File targetDirectory) {
        if (!grailsVersion.startsWith('2023') ||
                !(springBootVersion.substring(0, springBootVersion.lastIndexOf('.')) in SUPPORT_SPRING_BOOT_VERSIONS)) {
            // Currently already upgraded to Spring Boot 3.1.12
            return
        }

        String currentSpringBootVersion = new GrailsDependenciesDependencyManagement().springBootVersion
        boolean isCurrentSpringBootVersion = springBootVersion == currentSpringBootVersion
        boolean isMilestoneVersion = springBootVersion.contains('M') || springBootVersion.contains('RC')
        boolean isSnapshotVersion = springBootVersion.contains('SNAPSHOT')

        if (isCurrentSpringBootVersion) {
            ant.sequential {
                replace(file: 'build.gradle') {
                    replacetoken 'group '
                    replacevalue """ext['spring-framework.version'] = '6.0.23'

group """
                }
            }
            return
        }

        ant.sequential {
            replace(file: 'buildSrc/build.gradle') {
                replacetoken 'dependencies {'
                replacevalue """dependencies {
    implementation "org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion\""""
            }

            replace(file: 'build.gradle') {
                replacetoken 'group '
                replacevalue """ext['spring-boot.version'] = '$springBootVersion'

group """
            }

            if (isMilestoneVersion) {
                replace(file: 'buildSrc/build.gradle') {
                    replacetoken 'mavenCentral()'
                    replacevalue '''mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }'''
                }
                replace(file: 'build.gradle') {
                    replacetoken 'mavenCentral()'
                    replacevalue '''mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }'''
                }
            }
            else if (isSnapshotVersion) {
                replace(file: 'buildSrc/build.gradle') {
                    replacetoken 'mavenCentral()'
                    replacevalue '''mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
    maven { url 'https://repo.spring.io/snapshot' }'''
                }
                replace(file: 'build.gradle') {
                    replacetoken 'mavenCentral()'
                    replacevalue '''mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
    maven { url 'https://repo.spring.io/snapshot' }'''
                }
            }
        }

    }

    protected File getDestinationDirectory(File srcFile, File targetDirectory) {
        String searchDir = 'skeleton'
        File srcDir = srcFile.parentFile
        File destDir
        if (srcDir.absolutePath.endsWith(searchDir)) {
            destDir = targetDirectory
        }
        else {
            int index = srcDir.absolutePath.lastIndexOf(searchDir) + searchDir.size() + 1
            String relativePath = (srcDir.absolutePath - srcDir.absolutePath.substring(0, index))
            destDir = new File(targetDirectory, relativePath)
        }
        destDir
    }

    private Project createAntProject(String appName, File projectTargetDirectory, Map<String, String> properties,
                                     Map<String, String> args, GrailsConsole console, boolean verbose, boolean quiet = false) {
        GrailsConsoleAntProject project = new GrailsConsoleAntProject()
        project.setBaseDir(projectTargetDirectory)
        project.setName(appName)
        properties.each { k, v ->
            project.setProperty(k, v)
        }
        project.setOptions(args)
        ProjectHelper helper = ProjectHelper.getProjectHelper()
        helper.getImportStack().addElement("AntBuilder")
        project.addReference(MagicNames.REFID_PROJECT_HELPER, helper)
        BuildLogger logger = new GrailsConsoleLogger(console)
        if (verbose) {
            logger.setMessageOutputLevel(Project.MSG_DEBUG)
        }
        else {
            logger.setMessageOutputLevel(Project.MSG_ERR)
        }
        logger.setErrorPrintStream(console.err)
        logger.setOutputPrintStream(console.out)
        project.addBuildListener(logger)
        project.init()
        project.getBaseDir()

        project
    }

    @CompileDynamic
    private File unzipProfile(AntBuilder ant, Map<URL, File> unzippedDirectories, Resource location) {
        File tmpDir = null
        URL url = location.URL
        if (url && url.protocol == 'jar') {
            String absolutePath = url.path
            URL jarUrl = new URL(absolutePath.substring(0, absolutePath.lastIndexOf('!')))
            tmpDir = unzippedDirectories.get(jarUrl)

            if (tmpDir == null) {
                File jarFile = IOUtils.findJarFile(url)
                tmpDir = Files.createTempDirectory(UNZIP_PROFILE_TEMP_DIR).toFile()
                ant.unzip(src: jarFile, dest: tmpDir)
                unzippedDirectories.put(jarUrl, tmpDir)
            }
        }
        tmpDir
    }

    private List<GradleDependency> convertToGradleDependencies(List<Dependency> dependencies, String grailsVersion) {
        List<GradleDependency> gradleDependencies = []
        gradleDependencies.addAll(dependencies.collect {
            GrailsVersion.isGrails4(grailsVersion) || GrailsVersion.isGrails3(grailsVersion) ?
                    new GradleDependency(it, false) :
                    new GradleDependency(it)
        })
        gradleDependencies
    }

    private void buildTargetFolders(Profile profile, Map<Profile, File> targetDirs, File projectDir) {
        if (!targetDirs.containsKey(profile)) {
            targetDirs[profile] = projectDir
        }
        profile.extends.each { Profile p ->
            if (profile.parentSkeletonDir) {
                targetDirs[p] = profile.getParentSkeletonDir(projectDir)
            }
            else {
                targetDirs[p] = targetDirs[profile]
            }
            buildTargetFolders(p, targetDirs, projectDir)
        }
    }

    private String createNewApplicationYml(String previousYml, String newYml) {
        String ln = System.getProperty('line.separator')
        if (newYml != previousYml) {
            StringBuilder appended = new StringBuilder(previousYml.length() + newYml.length() + 30)
            if (!previousYml.startsWith('---')) {
                appended.append('---' + ln)
            }
            appended.append(previousYml).append(ln + '---' + ln)
            appended.append(newYml)
            appended.toString()
        }
        else {
            newYml
        }
    }

    private void appendFeatureFiles(File skeletonDir, File targetDirectory) {
        Set<File> ymlFiles = findAllFilesByName(skeletonDir, APPLICATION_YML)
        Set<File> buildGradleFiles = findAllFilesByName(skeletonDir, BUILD_GRADLE)
        Set<File> gradlePropertiesFiles = findAllFilesByName(skeletonDir, GRADLE_PROPERTIES)

        ymlFiles.each { File newYml ->
            File oldYml = new File(getDestinationDirectory(newYml, targetDirectory), APPLICATION_YML)
            String oldText = (oldYml.isFile()) ? oldYml.getText(ENCODING) : null
            if (oldText) {
                appendToYmlSubDocument(newYml, oldText, oldYml)
            }
            else {
                oldYml.text = newYml.getText(ENCODING)
            }
        }
        buildGradleFiles.each { File srcFile ->
            File destFile = new File(getDestinationDirectory(srcFile, targetDirectory), BUILD_GRADLE)
            destFile.text = destFile.getText(ENCODING) + System.lineSeparator() + srcFile.getText(ENCODING)
        }

        gradlePropertiesFiles.each { File srcFile ->
            File destFile = new File(getDestinationDirectory(srcFile, targetDirectory), GRADLE_PROPERTIES)
            if (!destFile.exists()) {
                destFile.createNewFile()
            }
            destFile.append(srcFile.getText(ENCODING))
        }
    }

    private void appendToYmlSubDocument(File applicationYmlFile, String previousApplicationYml) {
        appendToYmlSubDocument(applicationYmlFile, previousApplicationYml, applicationYmlFile)
    }

    private void appendToYmlSubDocument(File applicationYmlFile, String previousApplicationYml, File setTo) {
        String newApplicationYml = applicationYmlFile.text
        if (previousApplicationYml && newApplicationYml != previousApplicationYml) {
            setTo.text = createNewApplicationYml(previousApplicationYml, newApplicationYml)
        }
    }

    private Map<String, String> initializeVariables(String appName, String groupName, String packageName, String profileName,
                                                    List<Feature> features, String template, String grailsVersion) {
        Map<String, String> variables = new HashMap<>()
        Map<String, String> codegenVariables = getCodegenVariables(appName, groupName, packageName, profileName, features, template, grailsVersion)
        Map<String, String> dependencyVersions = getDependencyVersions(grailsVersion)
        variables.putAll(codegenVariables)
        variables.putAll(dependencyVersions)
        variables
    }

    private Map<String, Object> getApplicationTemplateBinding(String appName, String groupName, String packageName, Profile profile,
                                                              List<Feature> features, Map<String, String> args, String grailsVersion, String template) {
        ProjectContext projectContext = getProjectContext(appName, groupName, packageName, profile, features, grailsVersion, template)
        Map<String, String> dependencyVersions = getDependencyVersions(grailsVersion)
        Map<String, Object> binding = new HashMap<>()
        binding.put("project", projectContext)
        binding.put("versions", dependencyVersions)
        binding.put("options", args)
        binding
    }

    private ProjectContext getProjectContext(String appName, String groupName, String packageName, Profile profile,
                                             List<Feature> features, String grailsVersion, String template) {
        Map<String, String> codegenVariables = getCodegenVariables(appName, groupName, packageName, profile.name, features, template, grailsVersion)
        ProjectContext projectContext = new ProjectContext()
        projectContext.put('graceVersion', grailsVersion)
        projectContext.put('grailsVersion', grailsVersion)
        projectContext.putAll(codegenVariables)
        projectContext
    }

    private Map<String, String> getCodegenVariables(String appName, String groupName, String packageName, String profileName,
                                                    List<Feature> features, String template, String grailsVersion) {
        String projectClassName = GrailsNameUtils.getNameFromScript(appName)
        Map<String, String> variables = new HashMap<>()
        variables.APPNAME = appName

        variables['grails.codegen.defaultPackage'] = packageName
        variables['grails.codegen.defaultPackage.path'] = packageName.replace('.', '/')
        variables['grails.codegen.projectClassName'] = projectClassName
        variables['grails.codegen.projectName'] = GrailsNameUtils.getScriptName(projectClassName)
        variables['grails.codegen.projectNaturalName'] = GrailsNameUtils.getNaturalName(projectClassName)
        variables['grails.codegen.projectSnakeCaseName'] = GrailsNameUtils.getSnakeCaseName(projectClassName)
        variables['grails.profile'] = profileName
        variables['grails.version'] = grailsVersion
        variables['grails.app.name'] = appName
        variables['grails.app.group'] = groupName
        variables['grails.app.features'] = features*.name?.join(',')
        variables['grails.app.template'] = template ?: ''

        variables['grace.codegen.defaultPackage'] = packageName
        variables['grace.codegen.defaultPackage.path'] = packageName.replace('.', '/')
        variables['grace.codegen.projectClassName'] = projectClassName
        variables['grace.codegen.projectName'] = GrailsNameUtils.getScriptName(projectClassName)
        variables['grace.codegen.projectNaturalName'] = GrailsNameUtils.getNaturalName(projectClassName)
        variables['grace.codegen.projectSnakeCaseName'] = GrailsNameUtils.getSnakeCaseName(projectClassName)
        variables['grace.profile'] = profileName
        variables['grace.version'] = grailsVersion
        variables['grace.app.name'] = appName
        variables['grace.app.group'] = groupName
        variables['grace.app.features'] = features*.name?.join(',')
        variables['grace.app.template'] = template ?: ''

        variables
    }

    private Map<String, String> getDependencyVersions(String grailsVersion) {
        Map<String, String> versions = new HashMap<>()
        versions['grails.version'] = grailsVersion
        versions['grace.version'] = grailsVersion
        if (this.profileRepository instanceof MavenProfileRepository) {
            MavenProfileRepository mpr = (MavenProfileRepository) this.profileRepository
            String gormDep = mpr.profileDependencyVersions.getGormVersion()
            if (gormDep != null) {
                versions['gorm.version'] = gormDep
            }
            String groovyDep = mpr.profileDependencyVersions.getGroovyVersion()
            if (groovyDep != null) {
                versions['groovy.version'] = groovyDep
            }
            String grailsGradlePluginVersion = mpr.profileDependencyVersions.getGrailsGradlePluginVersion()
            if (grailsGradlePluginVersion != null) {
                versions['grails-gradle-plugin.version'] = grailsGradlePluginVersion
            }
            mpr.profileDependencyVersions.getProperties().each {
                versions[it.key.toString()] = it.value.toString()
            }
        }
        versions
    }

    private Map<String, String> getCommandArguments(CommandLine commandLine) {
        Map<String, String> commandArguments = new HashMap<>()
        commandLine.declaredOptions.each { name, value ->
            commandArguments.put(name, value.toString())
        }
        commandLine.undeclaredOptions.each { name, value ->
            commandArguments.put(name, value.toString())
        }
        if (!commandLine.hasOption(PROFILE_FLAG)) {
            commandArguments.put(PROFILE_FLAG, getDefaultProfile())
        }
        if (!commandLine.hasOption(STACKTRACE_ARGUMENT)) {
            commandArguments.put(STACKTRACE_ARGUMENT, 'false')
        }
        if (!commandLine.hasOption(VERBOSE_ARGUMENT)) {
            commandArguments.put(VERBOSE_ARGUMENT, 'false')
        }
        commandArguments
    }

    private Set<File> findAllFilesByName(File projectDir, String fileName) {
        Set<File> files = new HashSet<>()
        if (projectDir.exists()) {
            Files.walkFileTree(projectDir.absoluteFile.toPath(), new SimpleFileVisitor<Path>() {

                @Override
                FileVisitResult visitFile(Path path, BasicFileAttributes mainAtts)
                        throws IOException {
                    if (path.fileName.toString().endsWith(fileName)) {
                        files.add(path.toFile())
                    }
                    FileVisitResult.CONTINUE
                }

            })
        }
        files
    }

    private boolean isDirectoryEmpty(File target) {
        if (target.isDirectory()) {
            try (Stream<Path> entries = Files.list(Paths.get(target.toURI()))) {
                return !entries.findFirst().isPresent()
            }
        }
        false
    }

    private void deleteDirectoryOrFile(File file) {
        try {
            if (file.isDirectory()) {
                file?.deleteDir()
            }
            else if (file.isFile()) {
                file.delete()
            }
        }
        catch (Throwable ignored) {
            // Ignore error deleting temporal directory
        }
    }

    private static void cleanDirectory(File directory) throws IOException {
        FileFilter fileFilter = new FileFilter() {
            @Override
            boolean accept(File file) {
                // keep git directory to show changes between the versions
                if (file.isDirectory() && file.name == ".git") {
                    return false
                }
                return true
            }
        }
        File[] files = directory.listFiles(fileFilter)
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    file.deleteDir()
                }
                else {
                    file.delete()
                }
            } catch (IOException ignore) {
            }
        }
    }

    private static boolean validateSpecificGraceVersion(String specificGraceVersion) {
        specificGraceVersion != null && specificGraceVersion.substring(0, specificGraceVersion.indexOf('.')) in SUPPORT_GRACE_VERSIONS
    }

    static class CreateAppCommandObject {

        String appName
        File baseDir
        String profileName
        String grailsVersion
        String springBootVersion
        List<String> features
        String template
        boolean inplace = false
        boolean stacktrace = false
        boolean verbose = false
        boolean quiet = false
        boolean force = false
        GrailsConsole console
        Map<String, String> args

    }

    static class ProjectContext extends HashMap<String, Object> {

        boolean hasFeature(String feature) {
            String features = get('grails.app.features')
            if (features.contains(',')) {
                return features.split(',')?.contains(feature)
            }
            else {
                return features == feature
            }
        }

    }

}
