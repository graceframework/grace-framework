package org.grails.cli.profile.commands

import org.apache.tools.ant.BuildLogger
import org.apache.tools.ant.DefaultLogger
import org.apache.tools.ant.Location
import org.apache.tools.ant.MagicNames
import org.apache.tools.ant.Project
import org.apache.tools.ant.ProjectHelper
import org.apache.tools.ant.Target
import org.codehaus.groovy.ant.Groovy

import grails.util.BuildSettings
import org.grails.build.parsing.CommandLine
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProjectCommand

import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

class ApplicationTemplateCommand implements ProjectCommand {

    public static final String NAME = 'app:template'
    public static final String LOCATION_FLAG = 'location'
    CommandDescription description = new CommandDescription(name, 'Apply a template to an application', 'app-template --location=hello.groovy')

    ApplicationTemplateCommand() {
        populateDescription()
        description.flag(name: LOCATION_FLAG, description: 'The application template to apply', required: false)
        description.flag(name: STACKTRACE_ARGUMENT, description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, description: 'Show verbose output', required: false)
    }

    protected void populateDescription() {
//        description.argument(location: 'Application Template', description: 'The application template to apply.', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        CommandLine commandLine = executionContext.commandLine

        Project project = new Project()
        project.setBaseDir(BuildSettings.BASE_DIR)
        project.setName('App')
        ProjectHelper helper = ProjectHelper.getProjectHelper()
        project.addReference(MagicNames.REFID_PROJECT_HELPER, helper)
        BuildLogger logger = new DefaultLogger()
        if (commandLine.hasOption(VERBOSE_ARGUMENT)) {
            logger.setMessageOutputLevel(Project.MSG_DEBUG)
        }
        else {
            logger.setMessageOutputLevel(Project.MSG_INFO)
        }
        logger.setErrorPrintStream(executionContext.console.err)
        logger.setOutputPrintStream(executionContext.console.out)
        project.addBuildListener(logger)
        helper.getImportStack().addElement("AntBuilder")
        project.init()
        Target target = new Target()
        target.setProject(project)
        target.setName('CreateApp')
        target.setLocation(new Location(BuildSettings.BASE_DIR.canonicalPath))
        Groovy groovy = new Groovy()
        groovy.src = new File(commandLine.optionValue(LOCATION_FLAG).toString())
        groovy.setProject(project)
        groovy.setOwningTarget(target)
        groovy.execute()
        executionContext.console.println()

        return true
    }

}
