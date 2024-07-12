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
import org.apache.tools.ant.DefaultLogger
import org.apache.tools.ant.Location
import org.apache.tools.ant.MagicNames
import org.apache.tools.ant.Project
import org.apache.tools.ant.ProjectHelper
import org.apache.tools.ant.Target
import org.apache.tools.ant.types.ResourceCollection
import org.apache.tools.ant.types.resources.FileResource
import org.apache.tools.ant.types.resources.URLResource
import org.codehaus.groovy.ant.Groovy
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

import grails.build.logging.GrailsConsole
import grails.io.IOUtils
import grails.util.Environment
import grails.util.GrailsNameUtils
import grails.util.GrailsVersion
import org.grails.build.logging.GrailsConsoleAntBuilder
import org.grails.build.parsing.CommandLine
import org.grails.cli.GrailsCli
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Feature
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.commands.io.GradleDependency
import org.grails.cli.profile.repository.MavenProfileRepository
import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource

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

    private static final String GRAILS_VERSION_FALLBACK_IN_IDE_ENVIRONMENTS_FOR_RUNNING_TESTS = '2023.0.0-SNAPSHOT'
    public static final String NAME = 'create-app'
    public static final String PROFILE_FLAG = 'profile'
    public static final String FEATURES_FLAG = 'features'
    public static final String TEMPLATE_FLAG = 'template'
    public static final String ENCODING = System.getProperty('file.encoding') ?: 'UTF-8'
    public static final String INPLACE_FLAG = 'inplace'

    protected static final String APPLICATION_YML = 'application.yml'
    protected static final String BUILD_GRADLE = 'build.gradle'
    protected static final String GRADLE_PROPERTIES = 'gradle.properties'
    public static final String UNZIP_PROFILE_TEMP_DIR = 'grails-profile-'

    private final Map<URL, File> unzippedDirectories = new LinkedHashMap<URL, File>()

    ProfileRepository profileRepository
    Map<String, String> variables = [:]
    String appname
    String groupname
    String defaultpackagename
    File targetDirectory

    CommandDescription description = new CommandDescription(name, 'Creates an application', 'create-app [NAME] --profile=web')

    CreateAppCommand() {
        populateDescription()
        description.flag(name: INPLACE_FLAG, description: 'Used to create an application using the current directory')
        description.flag(name: PROFILE_FLAG, description: 'The profile to use', required: false)
        description.flag(name: FEATURES_FLAG, description: 'The features to use', required: false)
        description.flag(name: TEMPLATE_FLAG, description: 'The application template to use', required: false)
        description.flag(name: STACKTRACE_ARGUMENT, description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, description: 'Show verbose output', required: false)
    }

    protected void populateDescription() {
        description.argument(name: 'Application Name', description: 'The name of the application to create.', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    @Override
    protected int complete(CommandLine commandLine, CommandDescription desc, List<CharSequence> candidates, int cursor) {
        Map.Entry<String, Object> lastOption = commandLine.lastOption()
        if (lastOption != null) {
            // if value == true it means no profile is specified and only the flag is present
            List<String> profileNames = profileRepository.allProfiles*.name
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
                Profile profile = profileRepository.getProfile(commandLine.hasOption(PROFILE_FLAG) ?
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

    protected File getDestinationDirectory(File srcFile) {
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

    protected void appendFeatureFiles(File skeletonDir) {
        Set<File> ymlFiles = findAllFilesByName(skeletonDir, APPLICATION_YML)
        Set<File> buildGradleFiles = findAllFilesByName(skeletonDir, BUILD_GRADLE)
        Set<File> gradlePropertiesFiles = findAllFilesByName(skeletonDir, GRADLE_PROPERTIES)

        ymlFiles.each { File newYml ->
            File oldYml = new File(getDestinationDirectory(newYml), APPLICATION_YML)
            String oldText = (oldYml.isFile()) ? oldYml.getText(ENCODING) : null
            if (oldText) {
                appendToYmlSubDocument(newYml, oldText, oldYml)
            }
            else {
                oldYml.text = newYml.getText(ENCODING)
            }
        }
        buildGradleFiles.each { File srcFile ->
            File destFile = new File(getDestinationDirectory(srcFile), BUILD_GRADLE)
            destFile.text = destFile.getText(ENCODING) + System.lineSeparator() + srcFile.getText(ENCODING)
        }

        gradlePropertiesFiles.each { File srcFile ->
            File destFile = new File(getDestinationDirectory(srcFile), GRADLE_PROPERTIES)
            if (!destFile.exists()) {
                destFile.createNewFile()
            }
            destFile.append(srcFile.getText(ENCODING))
        }
    }

    protected void buildTargetFolders(Profile profile, Map<Profile, File> targetDir, File projectDir) {
        if (!targetDir.containsKey(profile)) {
            targetDir[profile] = projectDir
        }
        profile.extends.each { Profile p ->
            if (profile.parentSkeletonDir) {
                targetDir[p] = profile.getParentSkeletonDir(projectDir)
            }
            else {
                targetDir[p] = targetDir[profile]
            }
            buildTargetFolders(p, targetDir, projectDir)
        }
    }

    Set<File> findAllFilesByName(File projectDir, String fileName) {
        Set<File> files = (Set) []
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

    boolean handle(CreateAppCommandObject cmd) {
        if (profileRepository == null) {
            throw new IllegalStateException("Property 'profileRepository' must be set")
        }

        String profileName = cmd.profileName

        Profile profileInstance = profileRepository.getProfile(profileName)
        if (!validateProfile(profileInstance, profileName, cmd.console)) {
            return false
        }

        if (profileInstance) {
            if (!initializeGroupAndName(cmd.appName, cmd.inplace)) {
                return false
            }

            initializeVariables(profileName, cmd.grailsVersion)

            Path appFullDirectory = Paths.get(cmd.baseDir.path, appname)

            File projectTargetDirectory = cmd.inplace ? new File('.').canonicalFile : appFullDirectory.toAbsolutePath().normalize().toFile()

            if (projectTargetDirectory.exists() && !isDirectoryEmpty(projectTargetDirectory)) {
                cmd.console.error("Directory `${projectTargetDirectory.absolutePath}` is not empty!")
                return false
            }

            List<Feature> features = evaluateFeatures(profileInstance, cmd.features).toList()

            variables['grails.profile.features'] = features*.name?.sort()?.join(', ')
            variables['grace.profile.features'] = features*.name?.sort()?.join(', ')

            cmd.console.addStatus("Creating a new ${name == 'create-plugin' ? 'plugin' : 'application'}")
            cmd.console.println()
            cmd.console.println("     ${name == 'create-plugin' ? 'Plugin' : 'App'} name:".padRight(24) + appname)
            cmd.console.println("     Package name:".padRight(24) + defaultpackagename)
            cmd.console.println("     Profile:".padRight(24) + profileName)
            cmd.console.println("     Features:".padRight(24) + features*.name?.sort()?.join(', '))
            if (cmd.template) {
                cmd.console.println("     App template:".padRight(24) + cmd.template)
            }
            cmd.console.println("     Project location:".padRight(24) + projectTargetDirectory.absolutePath)
            cmd.console.println()

            List<Profile> profiles = profileRepository.getProfileAndDependencies(profileInstance)

            Map<Profile, File> targetDirs = [:]
            buildTargetFolders(profileInstance, targetDirs, projectTargetDirectory)

            for (Profile p : profiles) {
                Set<File> ymlFiles = findAllFilesByName(projectTargetDirectory, APPLICATION_YML)
                Map<File, String> ymlCache = [:]

                targetDirectory = targetDirs[p]

                ymlFiles.each { File applicationYmlFile ->
                    String previousApplicationYml = (applicationYmlFile.isFile()) ? applicationYmlFile.getText(ENCODING) : null
                    if (previousApplicationYml) {
                        ymlCache[applicationYmlFile] = previousApplicationYml
                    }
                }

                copySkeleton(profileInstance, p)

                ymlCache.each { File applicationYmlFile, String previousApplicationYml ->
                    if (applicationYmlFile.exists()) {
                        appendToYmlSubDocument(applicationYmlFile, previousApplicationYml)
                    }
                }
            }
            def ant = new GrailsConsoleAntBuilder()

            for (Feature f in features) {
                Resource location = f.location

                File skeletonDir
                File tmpDir
                if (location instanceof FileSystemResource) {
                    skeletonDir = location.createRelative('skeleton').file
                }
                else {
                    tmpDir = unzipProfile(ant, location)
                    skeletonDir = new File(tmpDir, "META-INF/grails-profile/features/$f.name/skeleton")
                }

                targetDirectory = targetDirs[f.profile]

                appendFeatureFiles(skeletonDir)

                if (skeletonDir.exists()) {
                    copySrcToTarget(ant, skeletonDir, ['**/' + APPLICATION_YML], profileInstance.binaryExtensions)
                }
            }

            // Cleanup temporal directories
            unzippedDirectories.values().each { File tmpDir ->
                deleteDirectory(tmpDir)
            }

            if (cmd.template) {
                if (cmd.template.endsWith('.groovy')) {
                    replaceBuildTokens(profileName, profileInstance, features, projectTargetDirectory)
                    applyApplicationTemplate(cmd.template, cmd.appName, projectTargetDirectory, cmd.console, cmd.verbose)
                }
                else if (cmd.template.endsWith('.zip') || cmd.template.endsWith('.git') || new File(cmd.template).isDirectory()) {
                    copyApplicationTemplate(ant, profileInstance, features, cmd.template, cmd.console)
                    replaceBuildTokens(profileName, profileInstance, features, projectTargetDirectory)
                }
            }
            else {
                replaceBuildTokens(profileName, profileInstance, features, projectTargetDirectory)
            }

            String grailsVersion = GrailsVersion.current().version
            cmd.console.addStatus(
                    "${name == 'create-plugin' ? 'Plugin' : 'Application'} created by Grace ${grailsVersion}."
            )
            if (profileInstance.instructions) {
                cmd.console.addStatus(profileInstance.instructions)
            }
            GrailsCli.triggerAppLoad()
            return true
        }

        System.err.println "Cannot find profile $profileName"
        false
    }

    private boolean isDirectoryEmpty(File target) {
        if (target.isDirectory()) {
            try (Stream<Path> entries = Files.list(Paths.get(target.toURI()))) {
                return !entries.findFirst().isPresent()
            }
        }
        false
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        CommandLine commandLine = executionContext.commandLine

        String profileName = evaluateProfileName(commandLine)

        List<String> validFlags = [INPLACE_FLAG, PROFILE_FLAG, FEATURES_FLAG, TEMPLATE_FLAG, STACKTRACE_ARGUMENT, VERBOSE_ARGUMENT]
        commandLine.undeclaredOptions.each { String key, Object value ->
            if (!validFlags.contains(key)) {
                List possibleSolutions = validFlags.findAll { it.substring(0, 2) == key.substring(0, 2) }
                StringBuilder warning = new StringBuilder("Unrecognized flag: ${key}.")
                if (possibleSolutions) {
                    warning.append(' Possible solutions: ')
                    warning.append(possibleSolutions.join(', '))
                }
                executionContext.console.warn(warning.toString())
            }
        }

        boolean inPlace = commandLine.hasOption('inplace') || GrailsCli.isInteractiveModeActive()
        String appName = commandLine.remainingArgs ? commandLine.remainingArgs[0] : ''

        List<String> features = commandLine.optionValue('features')?.toString()?.split(',')?.toList()

        CreateAppCommandObject cmd = new CreateAppCommandObject(
                appName: appName,
                baseDir: executionContext.baseDir,
                profileName: profileName,
                grailsVersion: Environment.getPackage().getImplementationVersion() ?: GRAILS_VERSION_FALLBACK_IN_IDE_ENVIRONMENTS_FOR_RUNNING_TESTS,
                features: features,
                template: commandLine.optionValue('template'),
                inplace: inPlace,
                stacktrace: commandLine.hasOption(STACKTRACE_ARGUMENT),
                verbose: commandLine.hasOption(VERBOSE_ARGUMENT),
                console: executionContext.console
        )

        this.handle(cmd)
    }

    protected boolean validateProfile(Profile profileInstance, String profileName, GrailsConsole console) {
        if (profileInstance == null) {
            console.error("Profile not found for name [$profileName]")
            return false
        }
        true
    }

    @CompileDynamic
    protected File unzipProfile(AntBuilder ant, Resource location) {
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

    @CompileDynamic
    protected void replaceBuildTokens(String profileCoords, Profile profile, List<Feature> features, File targetDirectory) {
        boolean isSnapshotVersion = GrailsVersion.current().isSnapshot()
        AntBuilder ant = new GrailsConsoleAntBuilder()
        String ln = System.getProperty('line.separator')

        Closure repositoryUrl = { int spaces, String repo ->
            repo.startsWith('http') ? "${' ' * spaces}maven { url \"${repo}\" }" : "${' ' * spaces}${repo}"
        }
        List<String> repositoryUrls = profile.repositories.sort().reverse()
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

        dependencies.add(new Dependency(profileRepository.getProfileArtifact(profileCoords), 'profile'))

        dependencies = dependencies.unique()

        List<GradleDependency> gradleDependencies = convertToGradleDependencies(dependencies)

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

        if (isSnapshotVersion) {
            buildRepositoryUrls.add(0, 'mavenLocal()')
        }
        String buildRepositoriesString = buildRepositoryUrls.collect(repositoryUrl.curry(4)).unique().join(ln)

        String buildDependenciesString = buildDependencies.collect { Dependency dep ->
            String artifactStr = resolveArtifactString(dep)
            "    implementation \"${artifactStr}\"".toString()
        }.unique().join(ln)

        List<GString> buildPlugins = profile.buildPlugins.collect { String name ->
            "    id \"$name\""
        }

        for (Feature f in features) {
            buildPlugins.addAll f.buildPlugins.collect { String name ->
                "    id \"$name\""
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

    protected String evaluateProfileName(CommandLine mainCommandLine) {
        mainCommandLine.optionValue('profile')?.toString() ?: getDefaultProfile()
    }

    protected Iterable<Feature> evaluateFeatures(Profile profile, List<String> requestedFeatures) {
        if (requestedFeatures) {
            List<String> allFeatureNames = profile.features*.name
            Collection<String> validFeatureNames = requestedFeatures.intersect(allFeatureNames)
            requestedFeatures.removeAll(allFeatureNames)
            requestedFeatures.each { String invalidFeature ->
                List possibleSolutions = allFeatureNames.findAll {
                    it.substring(0, 2) == invalidFeature.substring(0, 2)
                }
                StringBuilder warning = new StringBuilder("Feature ${invalidFeature} does not exist in the profile ${profile.name}!")
                if (possibleSolutions) {
                    warning.append(' Possible solutions: ')
                    warning.append(possibleSolutions.join(', '))
                }
                GrailsConsole.getInstance().warn(warning.toString())
            }
            return (profile.features.findAll { Feature f -> validFeatureNames.contains(f.name) } + profile.requiredFeatures).unique()
        }

        (profile.defaultFeatures + profile.requiredFeatures).unique()
    }

    protected String getDefaultProfile() {
        ProfileRepository.DEFAULT_PROFILE_NAME
    }

    protected String createNewApplicationYml(String previousYml, String newYml) {
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

    private void appendToYmlSubDocument(File applicationYmlFile, String previousApplicationYml) {
        appendToYmlSubDocument(applicationYmlFile, previousApplicationYml, applicationYmlFile)
    }

    private void appendToYmlSubDocument(File applicationYmlFile, String previousApplicationYml, File setTo) {
        String newApplicationYml = applicationYmlFile.text
        if (previousApplicationYml && newApplicationYml != previousApplicationYml) {
            setTo.text = createNewApplicationYml(previousApplicationYml, newApplicationYml)
        }
    }

    protected boolean initializeGroupAndName(String appName, boolean inplace) {
        if (!appName && !inplace) {
            GrailsConsole.getInstance().error('Specify an application name or use --inplace to create an application in the current directory')
            return false
        }
        String groupAndAppName = appName
        if (inplace) {
            appname = new File('.').canonicalFile.name
            groupAndAppName = groupAndAppName ?: appname
        }

        if (!groupAndAppName) {
            GrailsConsole.getInstance().error('Specify an application name or use --inplace to create an application in the current directory')
            return false
        }

        try {
            defaultpackagename = establishGroupAndAppName(groupAndAppName)
        }
        catch (IllegalArgumentException e) {
            GrailsConsole.instance.error(e.message)
            return false
        }
    }

    private void initializeVariables(String profileName, String grailsVersion) {
        Map<String, String> codegenVariables = getCodegenVariables(appname, groupname, defaultpackagename, profileName, grailsVersion)
        Map<String, String> dependencyVersions = getDependencyVersions(profileRepository, grailsVersion)
        variables.putAll(codegenVariables)
        variables.putAll(dependencyVersions)
    }

    private Map<String, String> getCodegenVariables(String appName, String groupName, String packageName, String profileName, String grailsVersion) {
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

        variables
    }

    private Map<String, String> getDependencyVersions(ProfileRepository profileRepository, String grailsVersion) {
        Map<String, String> versions = new HashMap<>()
        versions['grails.version'] = grailsVersion
        versions['grace.version'] = grailsVersion
        if (profileRepository instanceof MavenProfileRepository) {
            MavenProfileRepository mpr = (MavenProfileRepository) profileRepository
            String gormDep = mpr.profileDependencyVersions.getGormVersion()
            if (gormDep != null) {
                versions['gorm.version'] = gormDep
            }
            String groovyDep = mpr.profileDependencyVersions.getGroovyVersion()
            if (groovyDep != null) {
                versions['groovy.version'] = groovyDep
            }
            String grailsGradlePluginVersion = mpr.profileDependencyVersions.getGrailsVersion()
            if (grailsGradlePluginVersion != null) {
                versions['grails-gradle-plugin.version'] = grailsGradlePluginVersion
            }
            mpr.profileDependencyVersions.getProperties().each {
                versions[it.key.toString()] = it.value.toString()
            }
        }
        versions
    }

    private String establishGroupAndAppName(String groupAndAppName) {
        String defaultPackage
        List<String> parts = groupAndAppName.split(/\./) as List
        if (parts.size() == 1) {
            appname = parts[0]
            defaultPackage = createValidPackageName()
            groupname = defaultPackage
        }
        else {
            appname = parts[-1]
            groupname = parts[0..-2].join('.')
            defaultPackage = groupname
        }
        defaultPackage
    }

    private String createValidPackageName() {
        String defaultPackage = appname.split(/[-]+/).collect { String token ->
            (token.toLowerCase().toCharArray().findAll { char ch ->
                Character.isJavaIdentifierPart(ch)
            } as char[]) as String
        }.join('.')

        if (!GrailsNameUtils.isValidJavaPackage(defaultPackage)) {
            throw new IllegalArgumentException("Cannot create a valid package name for [$appname]. " +
                    'Please specify a name that is also a valid Java package.')
        }
        defaultPackage
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copySkeleton(Profile profile, Profile participatingProfile) {
        List<String> buildMergeProfileNames = profile.buildMergeProfileNames
        List<String> excludes = profile.skeletonExcludes
        if (profile == participatingProfile) {
            excludes = []
        }

        AntBuilder ant = new GrailsConsoleAntBuilder()

        Resource skeletonResource = participatingProfile.profileDir.createRelative('skeleton')
        File skeletonDir
        File tmpDir
        if (skeletonResource instanceof FileSystemResource) {
            skeletonDir = skeletonResource.file
        }
        else {
            // establish the JAR file name and extract
            tmpDir = unzipProfile(ant, skeletonResource)
            skeletonDir = new File(tmpDir, 'META-INF/grails-profile/skeleton')
        }
        copySrcToTarget(ant, skeletonDir, excludes, profile.binaryExtensions)

        Set<File> sourceBuildGradles = findAllFilesByName(skeletonDir, BUILD_GRADLE)

        sourceBuildGradles.each { File srcFile ->
            File srcDir = srcFile.parentFile
            File destDir = getDestinationDirectory(srcFile)
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
            File destDir = getDestinationDirectory(srcFile)
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
    protected void copySrcToTarget(GrailsConsoleAntBuilder ant, File srcDir, List excludes, Set<String> binaryFileExtensions) {
        ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
            fileSet(dir: srcDir, casesensitive: false) {
                exclude(name: '**/.gitkeep')
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
    protected void copyApplicationTemplate(GrailsConsoleAntBuilder ant, Profile profile, List<Feature> features, String templateUrl, GrailsConsole console) {
        File tempZipFile = null
        File tempDir = null
        File projectDir = null
        try {
            if (templateUrl.endsWith('.zip')) {
                tempZipFile = Files.createTempFile('grails-template-', '.zip').toFile()
                ant.get(src: templateUrl, dest: tempZipFile)

                tempDir = Files.createTempDirectory('grails-template-').toFile()
                ant.unzip(src: tempZipFile, dest: tempDir)

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
                tempDir = Files.createTempDirectory('grails-template-').toFile()
                ant.exec(executable: 'git') {
                    arg value: 'clone'
                    arg value: templateUrl
                    arg value: tempDir
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
            } else {
                projectDir = new File(templateUrl)
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

            String grailsVersion = GrailsVersion.current().version
            Map<String, String> codegenVariables = getCodegenVariables(appname, groupname, defaultpackagename, profile.name, grailsVersion)
            Map<String, String> dependencyVersions = getDependencyVersions(profileRepository, grailsVersion)
            Map<String, Object> project = new HashMap<>()
            project.put('appName', appname)
            project.put('packageName', defaultpackagename)
            project.put('profile', profile.name)
            project.put('features', features*.name.sort())
            project.put('template', templateUrl)
            project.put('graceVersion', grailsVersion)
            project.put('grailsVersion', grailsVersion)
            project.putAll(codegenVariables)
            Map<String, Object> binding = new HashMap<>()
            binding.put("project", project)
            binding.put("versions", dependencyVersions)

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

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()))
            Map<String, Object> templateConfig = yaml.<Map<String, Object>> load(new FileInputStream(projectYml))
            List<String> excludes = ['**/*.tpl']
            Set<String> binaryFileExtensions = (profile.binaryExtensions + (Set<String>) templateConfig.getOrDefault('template.binaryExtensions', [])).unique()
            Set<String> executablePatterns = (profile.executablePatterns + (Set<String>) templateConfig.getOrDefault('template.executablePatterns', [])).unique()

            ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
                fileSet(dir: templateDir, casesensitive: false) {
                    exclude(name: '**/.gitkeep')
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
        }
        catch (Exception ex) {
            console.error("Can not apply template `${templateUrl}`!", ex)
        }
        finally {
            // Cleanup temporal directories
            tempZipFile?.deleteOnExit()
            deleteDirectory(tempDir)
        }
    }

    protected void applyApplicationTemplate(String template, String appName, File projectTargetDirectory, GrailsConsole console, boolean verbose) {
        ResourceCollection resource
        if (template.startsWith('http://') || template.startsWith('https://') || template.startsWith('file://')) {
            resource = new URLResource(template)
        }
        else {
            File file = new File(template)
            resource = new FileResource(file)
        }
        Location location = new Location(projectTargetDirectory.absolutePath)
        Project project = new Project()
        project.setBaseDir(projectTargetDirectory)
        project.setName(appName)
        variables.each { k, v ->
            project.setProperty(k, v)
        }
        ProjectHelper helper = ProjectHelper.getProjectHelper()
        helper.getImportStack().addElement("AntBuilder")
        project.addReference(MagicNames.REFID_PROJECT_HELPER, helper)
        BuildLogger logger = new DefaultLogger()
        if (verbose) {
            logger.setMessageOutputLevel(Project.MSG_DEBUG)
        } else {
            logger.setMessageOutputLevel(Project.MSG_INFO)
        }
        logger.setErrorPrintStream(console.err)
        logger.setOutputPrintStream(console.out)
        project.addBuildListener(logger)
        project.init()
        Target target = new Target()
        target.setProject(project)
        target.setName('CreateApp')
        target.setLocation(location)
        Groovy groovy = new Groovy()
        groovy.addConfigured(resource)
        groovy.setProject(project)
        groovy.setLocation(location)
        groovy.setOwningTarget(target)
        groovy.execute()
        console.println()
    }

    protected String resolveArtifactString(Dependency dep) {
        Artifact artifact = dep.artifact
        String v = artifact.version.replace('BOM', '')

        v ? "${artifact.groupId}:${artifact.artifactId}:${v}" : "${artifact.groupId}:${artifact.artifactId}"
    }

    private void deleteDirectory(File directory) {
        try {
            directory?.deleteDir()
        }
        catch (Throwable ignored) {
            // Ignore error deleting temporal directory
        }
    }

    protected List<GradleDependency> convertToGradleDependencies(List<Dependency> dependencies) {
        List<GradleDependency> gradleDependencies = []
        gradleDependencies.addAll(dependencies.collect { new GradleDependency(it) })
        gradleDependencies
    }

    static class CreateAppCommandObject {

        String appName
        File baseDir
        String profileName
        String grailsVersion
        List<String> features
        String template
        boolean inplace = false
        boolean stacktrace = false
        boolean verbose = false
        GrailsConsole console

    }

}
