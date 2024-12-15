package grails.cli.generator;

import org.grails.build.parsing.CommandLine;

public class GenerationContext {
    private CommandLine commandLine;

    public CommandLine getCommandLine() {
        return this.commandLine;
    }

    public void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

}
