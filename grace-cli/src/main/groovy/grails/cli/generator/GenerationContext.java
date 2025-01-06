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
package grails.cli.generator;

import java.io.File;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;

import org.grails.build.parsing.CommandLine;

/**
 * @author Michael Yan
 * @since 2023.2.0
 */
public class GenerationContext {

    protected File baseDir = BuildSettings.BASE_DIR;
    protected GrailsConsole console;
    protected CommandLine commandLine;

    public File getBaseDir() {
        return this.baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public GrailsConsole getConsole() {
        return this.console;
    }

    public void setConsole(GrailsConsole console) {
        this.console = console;
    }

    public CommandLine getCommandLine() {
        return this.commandLine;
    }

    public void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

}
