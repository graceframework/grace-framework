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
package org.grails.cli.profile.commands

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.tools.ant.BuildLogger
import org.apache.tools.ant.MagicNames
import org.apache.tools.ant.Project
import org.apache.tools.ant.ProjectHelper
import org.apache.tools.ant.types.ResourceCollection
import org.apache.tools.ant.types.resources.FileResource
import org.apache.tools.ant.types.resources.URLResource

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.util.GrailsVersion
import org.grails.build.logging.GrailsConsoleAntBuilder
import org.grails.build.logging.GrailsConsoleAntProject
import org.grails.build.logging.GrailsConsoleLogger
import org.grails.build.parsing.CommandLine
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.ProjectCommand
import org.grails.cli.profile.ProjectContext
import org.grails.cli.profile.ProjectContextAware
import org.grails.cli.profile.repository.MavenProfileRepository
import org.grails.config.CodeGenConfig

import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Apply a application template to an existing Grace project.
 *
 * Application templates are simple Groovy files containing DSL, Ant tasks, scripts etc.
 * <p>
 * you can use templates to generate/customize Grace applications.
 * you can write reusable application templates using the Groovy template scripts.
 *
 * @author Michael Yan
 * @since 2023.0
 */
@CompileStatic
class ApplicationTemplateCommand implements ProjectCommand, ProjectContextAware, ProfileRepositoryAware {

    public static final String NAME = 'template'
    public static final String LOCATION_FLAG = 'location'
    CommandDescription description = new CommandDescription(NAME,
            'Apply a template to an exist application',
            'grace app:template --location=http://example.com/template.groovy')

    String namespace = 'app'
    ProfileRepository profileRepository
    ProjectContext projectContext

    ApplicationTemplateCommand() {
        populateDescription()
    }

    protected void populateDescription() {
        description.flag(name: LOCATION_FLAG, description: 'The application template to apply', required: false)
        description.flag(name: STACKTRACE_ARGUMENT, description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, description: 'Show verbose output', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        String template = commandLine.optionValue(LOCATION_FLAG)
        boolean verbose = commandLine.hasOption(VERBOSE_ARGUMENT)

        applyApplicationTemplate(console, template, verbose)

        return true
    }

    @CompileDynamic
    private void applyApplicationTemplate(GrailsConsole console, String template, boolean verbose) {
        console.addStatus('Applying Template')
        console.println()

        CodeGenConfig applicationConfig = loadApplicationConfig()
        String grailsVersion = GrailsVersion.current().version
        String appName = applicationConfig.get('info.app.name')
        String defaultPackageName = applicationConfig.get('grails.codegen.defaultPackage')
        String groupName = defaultPackageName
        String profileName = applicationConfig.get('grails.profile', 'web')
        File targetDirectory = BuildSettings.BASE_DIR

        Map<String, String> variables = initializeVariables(appName, groupName, defaultPackageName, profileName, template, grailsVersion)

        GrailsConsoleAntBuilder ant = new GrailsConsoleAntBuilder(createAntProject(appName, targetDirectory, variables, console, verbose))

        ant.taskdef(resource: 'org/grails/cli/profile/tasks/antlib.xml')

        if (!verbose) {
            ant.setLoggerLevel(Project.MSG_INFO)
        }
        ResourceCollection resource
        if (template.startsWith('http://') || template.startsWith('https://') || template.startsWith('file://')) {
            resource = new URLResource(template)
            if (!resource.isExists()) {
                console.error("Template resource `${template}` is not exists!\n")
                return
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
            ant.groovy(src: template)
        }
        if (!verbose) {
            ant.setLoggerLevel(Project.MSG_ERR)
        }
        console.println()
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

    private Project createAntProject(String appName, File projectTargetDirectory, Map<String, String> properties,
                                     GrailsConsole console, boolean verbose = false) {
        GrailsConsoleAntProject project = new GrailsConsoleAntProject()
        project.setBaseDir(projectTargetDirectory)
        project.setName(appName)
        properties.each { k, v ->
            project.setProperty(k, v)
        }
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

    private Map<String, String> initializeVariables(String appName, String groupName, String packageName, String profileName,
                                                    String template, String grailsVersion) {
        Map<String, String> variables = new HashMap<>()
        Map<String, String> codegenVariables = getCodegenVariables(appName, groupName, packageName, profileName, template, grailsVersion)
        Map<String, String> dependencyVersions = getDependencyVersions(grailsVersion)
        variables.putAll(codegenVariables)
        variables.putAll(dependencyVersions)
        variables
    }

    private Map<String, String> getCodegenVariables(String appName, String groupName, String packageName, String profileName,
                                                    String template, String grailsVersion) {
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

}
