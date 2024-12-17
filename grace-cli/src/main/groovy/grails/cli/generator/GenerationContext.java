package grails.cli.generator;

import java.io.File;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;
import org.grails.build.parsing.CommandLine;

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
