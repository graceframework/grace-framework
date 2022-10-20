/*
 * Copyright 2021-2022 the original author or authors.
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
package org.grails.boot.context;

import java.io.File;

import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationListener;

import grails.util.BuildSettings;

/**
 * An {@link ApplicationListener} that saves Grails application PID into file.
 *
 * @author Michael Yan
 * @since 2022.0.0
 * @see ApplicationPidFileWriter
 */
public class GrailsApplicationPidFileWriter extends ApplicationPidFileWriter {

    private static final String DEFAULT_FILE_NAME = "grails.pid";

    public GrailsApplicationPidFileWriter() {
        super(new File(BuildSettings.TARGET_DIR, DEFAULT_FILE_NAME));
    }

}
