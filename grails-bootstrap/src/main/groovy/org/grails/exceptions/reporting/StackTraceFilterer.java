/*
 * Copyright 2011-2022 the original author or authors.
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
package org.grails.exceptions.reporting;

/**
 * Improves the output of stack traces produced by exceptions in a Grails application.
 *
 * @since 2.0
 * @author Graeme Rocher
 */
public interface StackTraceFilterer {

    String FULL_STACK_TRACE_MESSAGE = "Full Stack Trace:";

    String SYS_PROP_DISPLAY_FULL_STACKTRACE = "grails.full.stacktrace";

    /**
     * Adds a package name that should be filtered
     *
     * @param name The name of the package
     */
    void addInternalPackage(String name);

    /**
     * Sets the package where the stack trace should end
     * @param cutOffPackage The cut off package
     */
    void setCutOffPackage(String cutOffPackage);

    /**
     * <p>Remove all apparently Grails-internal trace entries from the exception instance<p>
     * <p>This modifies the original instance and returns it, it does not clone</p>
     * @param source The source exception
     * @param recursive Whether to recursively filter the cause
     * @return The exception passed in, after cleaning the stack trace
     */
    Throwable filter(Throwable source, boolean recursive);

    /**
     * <p>Remove all apparently Grails-internal trace entries from the exception instance<p>
     * <p>This modifies the original instance and returns it, it does not clone</p>
     * @param source The source exception
     * @return The exception passed in, after cleaning the stack trace
     */
    Throwable filter(Throwable source);

    /**
     * @param shouldFilter Whether to filter stack traces or not
     */
    void setShouldFilter(boolean shouldFilter);

}
