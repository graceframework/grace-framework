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
package org.grails.cli.profile.tasks;

import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.taskdefs.ExecTask;

/**
 * @author Michael Yan
 * @since 2023.0
 */
public class Git extends ExecTask {

    public Git() {
        super.setExecutable("git");
    }

    public void maybeConfigure() throws BuildException {
        RuntimeConfigurable configurator = getRuntimeConfigurableWrapper();
        Hashtable<String, Object> attributes = configurator.getAttributeMap();
        attributes.forEach((name, value) -> {
            createArg().setValue(name);
            createArg().setValue(value.toString());
        });
    }

    @Override
    public void execute() throws BuildException {
        StringBuilder command = new StringBuilder();
        getWrapper().getAttributeMap().forEach((name, value) -> command.append(name).append(' ').append(value));
        log(command.toString());
        super.execute();
    }

}
