/*
 * Copyright 2011-2023 the original author or authors.
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
package org.grails.build.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import grails.util.Environment;

/**
 * Command line parser that parses arguments to the command line. Written as a
 * replacement for Commons CLI because it doesn't support unknown arguments and
 * requires all arguments to be declared up front.
 * It also doesn't support command options with hyphens. This class gets around those problems.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CommandLineParser {

    static final Map<String, String> ENV_ARGS = new HashMap<>();

    static final Map<String, String> DEFAULT_ENVS = new HashMap<>();

    private static CommandLine CURRENT = null;

    private static final String DEFAULT_PADDING = "        ";

    static {
        ENV_ARGS.put("dev", Environment.DEVELOPMENT.getName());
        ENV_ARGS.put("prod", Environment.PRODUCTION.getName());
        ENV_ARGS.put("test", Environment.TEST.getName());
        DEFAULT_ENVS.put("war", Environment.PRODUCTION.getName());
        DEFAULT_ENVS.put("test-app", Environment.TEST.getName());
    }

    private final Map<String, Option> declaredOptions = new HashMap<>();

    private int longestOptionNameLength = 0;

    public static CommandLine getCurrentCommandLine() {
        return CURRENT;
    }

    /**
     * Adds a declared option
     *
     * @param name The name of the option
     * @param description The description
     */
    public void addOption(String name, String description) {
        int length = name.length();
        if (length > this.longestOptionNameLength) {
            this.longestOptionNameLength = length;
        }
        this.declaredOptions.put(name, new Option(name, description));
    }

    /**
     * Parses a string of all the command line options converting them into an array of arguments to pass to #parse(String..args)
     *
     * @param string The string
     * @return The command line
     */
    public CommandLine parseString(String string) {
        // Steal ants implementation for argument splitting. Handles quoted arguments with " or '.
        // Doesn't handle escape sequences with \
        return parse(translateCommandline(string));
    }

    /**
     * Crack a command line.
     * @param toProcess the command line to process.
     * @return the command line broken into strings.
     * An empty or null toProcess parameter results in a zero sized array.
     */
    public static String[] translateCommandline(String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            //no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    }
                    else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    }
                    else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    }
                    else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    }
                    else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() != 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                    }
                    else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() != 0) {
            result.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
            throw new ParseException("unbalanced quotes in " + toProcess);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Parses a string of all the command line options converting them into an array of arguments to pass to #parse(String..args)
     *
     *  @param commandName The command name
     * @param args The string
     * @return The command line
     */
    public CommandLine parseString(String commandName, String args) {
        // Steal ants implementation for argument splitting. Handles quoted arguments with " or '.
        // Doesn't handle escape sequences with \
        String[] argArray = translateCommandline(args);
        DefaultCommandLine cl = createCommandLine();
        cl.setCommandName(commandName);
        parseInternal(cl, argArray, false);
        return cl;
    }

    /**
     * Parses the given list of command line arguments. Arguments starting with -D become system properties,
     * arguments starting with -- or - become either declared or undeclared options. All other arguments are
     * put into a list of remaining arguments
     *
     * @param args The arguments
     * @return The command line state
     */
    public CommandLine parse(String... args) {
        DefaultCommandLine cl = createCommandLine();
        return parse(cl, args);
    }

    public CommandLine parse(DefaultCommandLine cl, String[] args) {
        parseInternal(cl, args, true);
        return cl;
    }

    private void parseInternal(DefaultCommandLine cl, String[] args, boolean firstArgumentIsCommand) {
        cl.setRawArguments(args);
        String lastWasOption = null;
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String trimmed = arg.trim();
            if (trimmed.length() > 0) {
                if (trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
                    trimmed = trimmed.substring(1, trimmed.length() - 1);
                }
                if (trimmed.charAt(0) == '-') {
                    lastWasOption = processOption(cl, trimmed);
                }
                else {
                    if (lastWasOption != null) {
                        cl.addUndeclaredOption(lastWasOption, trimmed);
                        lastWasOption = null;
                        continue;
                    }
                    if (firstArgumentIsCommand && ENV_ARGS.containsKey(trimmed)) {
                        cl.setEnvironment(ENV_ARGS.get(trimmed));
                    }
                    else {
                        if (firstArgumentIsCommand) {
                            cl.setCommandName(trimmed);
                            firstArgumentIsCommand = false;
                        }
                        else {
                            cl.addRemainingArg(trimmed);
                        }
                    }
                }
            }
        }
    }

    public String getOptionsHelpMessage() {
        String ls = System.getProperty("line.separator");
        String usageMessage = "Available options:";
        StringBuilder sb = new StringBuilder(usageMessage);
        sb.append(ls);
        for (Option option : this.declaredOptions.values()) {
            String name = option.getName();
            int extraPadding = this.longestOptionNameLength - name.length();
            sb.append(" -").append(name);
            for (int i = 0; i < extraPadding; i++) {
                sb.append(' ');
            }
            sb.append(DEFAULT_PADDING).append(option.getDescription()).append(ls);
        }

        return sb.toString();
    }

    protected DefaultCommandLine createCommandLine() {
        DefaultCommandLine defaultCommandLine = new DefaultCommandLine();
        CURRENT = defaultCommandLine;
        return defaultCommandLine;
    }

    protected String processOption(DefaultCommandLine cl, String arg) {
        if (arg.length() < 2) {
            return null;
        }

        if (arg.charAt(1) == 'D' && arg.contains("=")) {
            processSystemArg(cl, arg);
            return null;
        }

        arg = (arg.charAt(1) == '-' ? arg.substring(2) : arg.substring(1)).trim();

        if (arg.contains("=")) {
            String[] split = arg.split("=");
            String name = split[0].trim();
            validateOptionName(name);
            String value = split[1].trim();
            if (this.declaredOptions.containsKey(name)) {
                cl.addDeclaredOption(name, this.declaredOptions.get(name), value);
            }
            else {
                cl.addUndeclaredOption(name, value);
            }
            return null;
        }

        validateOptionName(arg);
        if (this.declaredOptions.containsKey(arg)) {
            cl.addDeclaredOption(arg, this.declaredOptions.get(arg));
        }
        else {
            cl.addUndeclaredOption(arg);
        }
        return arg;
    }

    private void validateOptionName(String name) {
        if (name.contains(" ")) {
            throw new ParseException("Invalid argument: " + name);
        }
    }

    protected void processSystemArg(DefaultCommandLine cl, String arg) {
        int i = arg.indexOf('=');
        String name = arg.substring(2, i);
        String value = arg.substring(i + 1);
        cl.addSystemProperty(name, value);
    }

}
