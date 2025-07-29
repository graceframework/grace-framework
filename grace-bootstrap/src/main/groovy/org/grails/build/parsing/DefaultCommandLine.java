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
package org.grails.build.parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import grails.util.Environment;

/**
 * Implementation of the {@link CommandLine} interface.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
public class DefaultCommandLine implements CommandLine {

    Properties systemProperties = new Properties();

    LinkedHashMap<String, Object> undeclaredOptions = new LinkedHashMap<>();

    LinkedHashMap<String, SpecifiedOption> declaredOptions = new LinkedHashMap<>();

    List<String> remainingArgs = new ArrayList<>();

    private String environment;

    private String commandName;

    private String[] rawArguments;

    public void addDeclaredOption(String name, Option option) {
        addDeclaredOption(name, option, Boolean.TRUE);
    }

    public void addUndeclaredOption(String option) {
        this.undeclaredOptions.put(option, Boolean.TRUE);
    }

    public void addUndeclaredOption(String option, Object value) {
        this.undeclaredOptions.put(option, value);
    }

    public void addDeclaredOption(String name, Option option, Object value) {
        SpecifiedOption so = new SpecifiedOption();
        so.option = option;
        so.value = value;

        this.declaredOptions.put(name, so);
    }

    @Override
    public CommandLine parseNew(String[] args) {
        DefaultCommandLine defaultCommandLine = new DefaultCommandLine();
        defaultCommandLine.systemProperties.putAll(this.systemProperties);
        defaultCommandLine.undeclaredOptions.putAll(this.undeclaredOptions);
        defaultCommandLine.declaredOptions.putAll(this.declaredOptions);
        CommandLineParser parser = new CommandLineParser();
        return parser.parse(defaultCommandLine, args);
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
        System.setProperty(Environment.KEY, environment);
    }

    public void setCommand(String name) {
        this.commandName = name;
    }

    public String getEnvironment() {
        boolean useDefaultEnv = this.environment == null;
        String env;
        if (useDefaultEnv && this.commandName != null) {
            env = lookupEnvironmentForCommand();
        }
        else {
            String fallbackEnv = System.getProperty(Environment.KEY) != null ? System.getProperty(Environment.KEY) :
                    Environment.DEVELOPMENT.getName();
            env = this.environment != null ? this.environment : fallbackEnv;
        }

        System.setProperty(Environment.KEY, env);
        System.setProperty(Environment.DEFAULT, String.valueOf(useDefaultEnv));

        return env;
    }

    public String lookupEnvironmentForCommand() {
        String fallbackEnv = System.getProperty(Environment.KEY) != null ? System.getProperty(Environment.KEY) : Environment.DEVELOPMENT.getName();
        String env = CommandLineParser.DEFAULT_ENVS.get(this.commandName);
        return env == null ? fallbackEnv : env;
    }

    public boolean isEnvironmentSet() {
        return this.environment != null;
    }

    public void setCommandName(String cmd) {
        if (REFRESH_DEPENDENCIES_ARGUMENT.equals(cmd)) {
            addUndeclaredOption(REFRESH_DEPENDENCIES_ARGUMENT);
        }
        this.commandName = cmd;
    }

    public String getCommandName() {
        return this.commandName;
    }

    public void addRemainingArg(String arg) {
        this.remainingArgs.add(arg);
    }

    public List<String> getRemainingArgs() {
        return this.remainingArgs;
    }

    public String[] getRemainingArgsArray() {
        return this.remainingArgs.toArray(new String[0]);
    }

    public Properties getSystemProperties() {
        return this.systemProperties;
    }

    public boolean hasOption(String name) {
        return this.declaredOptions.containsKey(name) || this.undeclaredOptions.containsKey(name);
    }

    public Object optionValue(String name) {
        if (this.declaredOptions.containsKey(name)) {
            SpecifiedOption specifiedOption = this.declaredOptions.get(name);
            return specifiedOption.value;
        }
        if (this.undeclaredOptions.containsKey(name)) {
            return this.undeclaredOptions.get(name);
        }
        return null;
    }

    @Override
    public Map.Entry<String, Object> lastOption() {
        Iterator<Map.Entry<String, Object>> i = this.undeclaredOptions.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Object> next = i.next();
            if (!i.hasNext()) {
                return next;
            }
        }
        return null;
    }

    public String getRemainingArgsString() {
        return remainingArgsToString(" ", false);
    }

    @Override
    public String getRemainingArgsWithOptionsString() {
        return remainingArgsToString(" ", true);
    }

    public String getRemainingArgsLineSeparated() {
        return remainingArgsToString("\n", false);
    }

    private String remainingArgsToString(String separator, boolean includeOptions) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        List<String> args = new ArrayList<>(this.remainingArgs);
        if (includeOptions) {
            for (Map.Entry<String, Object> entry : this.undeclaredOptions.entrySet()) {
                if (entry.getValue() instanceof Boolean && ((Boolean) entry.getValue())) {
                    args.add('-' + entry.getKey());
                }
                else {
                    args.add('-' + entry.getKey() + '=' + entry.getValue());
                }
            }
        }
        for (String arg : args) {
            sb.append(sep).append(arg);
            sep = separator;
        }
        return sb.toString();
    }

    public Map<String, Object> getDeclaredOptions() {
        Map<String, Object> declaredOptions = new LinkedHashMap<>();

        for (Map.Entry<String, SpecifiedOption> entry : this.declaredOptions.entrySet()) {
            declaredOptions.put(entry.getKey(), entry.getValue().getValue());
        }
        return declaredOptions;
    }

    public Map<String, Object> getUndeclaredOptions() {
        return this.undeclaredOptions;
    }

    public void addSystemProperty(String name, String value) {
        if (Environment.KEY.equals(name)) {
            setEnvironment(value);
        }
        this.systemProperties.put(name, value);
    }

    public void setRawArguments(String[] args) {
        this.rawArguments = args;
    }

    @Override
    public String[] getRawArguments() {
        return this.rawArguments;
    }

    public static class SpecifiedOption {

        private Option option;

        private Object value;

        public Option getOption() {
            return this.option;
        }

        public Object getValue() {
            return this.value;
        }

    }

}
