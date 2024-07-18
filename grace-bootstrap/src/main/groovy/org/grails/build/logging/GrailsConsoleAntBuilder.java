/*
 * Copyright 2011-2024 the original author or authors.
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
package org.grails.build.logging;

import groovy.ant.AntBuilder;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.LogLevel;

import grails.build.logging.GrailsConsole;

/**
 * Silences ant builder output.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConsoleAntBuilder extends AntBuilder {

    private final GrailsConsole console;

    public GrailsConsoleAntBuilder() {
        this(createAntProject());
    }

    public GrailsConsoleAntBuilder(GrailsConsole console) {
        super(createAntProject());
        this.console = console;
    }

    public GrailsConsoleAntBuilder(Project project) {
        super(project);
        this.console = GrailsConsole.getInstance();
    }

    public GrailsConsoleAntBuilder(GrailsConsole console, Project project) {
        super(project);
        this.console = console;
    }

    public GrailsConsoleAntBuilder(Task parentTask) {
        super(parentTask);
        this.console = GrailsConsole.getInstance();
    }

    /**
     * @return Factory method to create new Project instances
     */
    protected static Project createAntProject() {
        Project project = new Project();

        ProjectHelper helper = ProjectHelper.getProjectHelper();
        project.addReference(MagicNames.REFID_PROJECT_HELPER, helper);
        helper.getImportStack().addElement("AntBuilder"); // import checks that stack is not empty

        addGrailsConsoleBuildListener(project);

        project.init();
        project.getBaseDir();
        return project;
    }

    public void setLoggerLevel(int level) {
        for (Object buildListener : getProject().getBuildListeners()) {
            if (buildListener instanceof BuildLogger) {
                ((BuildLogger) buildListener).setMessageOutputLevel(level);
            }
        }
    }

    public static void addGrailsConsoleBuildListener(Project project) {
        BuildLogger logger = new GrailsConsoleLogger();

        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);

        project.addBuildListener(logger);

        GrailsConsole instance = GrailsConsole.getInstance();
        project.addBuildListener(new GrailsConsoleBuildListener(instance));

        if (!instance.isVerbose()) {
            for (Object buildListener : project.getBuildListeners()) {
                if (buildListener instanceof BuildLogger) {
                    ((BuildLogger) buildListener).setMessageOutputLevel(LogLevel.ERR.getLevel());
                }
            }
        }
    }

}
