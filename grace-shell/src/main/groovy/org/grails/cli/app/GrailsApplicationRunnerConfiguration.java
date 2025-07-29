/*
 * Copyright 2012-2023 the original author or authors.
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
package org.grails.cli.app;

import java.util.logging.Level;

import org.grails.cli.compiler.GroovyCompilerConfiguration;

/**
 * Configuration for the {@link GrailsApplicationRunner}.
 *
 * @author Phillip Webb
 * @author Michael Yan
 * @since 2022.1.0
 */
public interface GrailsApplicationRunnerConfiguration extends GroovyCompilerConfiguration {

    /**
     * Returns {@code true} if the source file should be monitored for changes and
     * automatically recompiled.
     *
     * @return {@code true} if file watching should be performed, otherwise {@code false}
     */
    boolean isWatchForFileChanges();

    /**
     * Returns the logging level to use.
     *
     * @return the logging level
     */
    Level getLogLevel();

}
