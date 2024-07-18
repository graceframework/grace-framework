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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Echo;

/**
 * @author Michael Yan
 * @since 2023.0
 */
public class File extends Echo {

    public File() {
    }

    public void setName(java.io.File name) {
        setFile(name);
    }

    @Override
    public void execute() throws BuildException {
        log(file.getName());
        super.execute();
    }
}
