/*
 * Copyright 2015-2025 the original author or authors.
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
package grails.views

/**
 * Interface for scripts that are writable
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
interface WritableScript extends Writable, WriterProvider {

    /**
     * Obtains the source file
     */
    File getSourceFile()

    /**
     * @param file Sets the source file
     */
    void setSourceFile(File file)

    /**
     * Sets the binding
     *
     * @param binding The binding
     */
    void setBinding(Binding binding)

    /**
     * @return Obtains the binding
     */
    Binding getBinding()

    /**
     * Runs the script and returns the result
     *
     * @return The result
     */
    Object run()

}
