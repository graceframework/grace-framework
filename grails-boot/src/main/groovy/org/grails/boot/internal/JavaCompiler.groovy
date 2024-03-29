/*
 * Copyright 2015-2023 the original author or authors.
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
package org.grails.boot.internal

import java.nio.charset.StandardCharsets

import javax.tools.StandardJavaFileManager
import javax.tools.ToolProvider

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.util.ClassUtils

/**
 * Helper for recompiling Java code at runtime
 *
 * @author Graeme Rocher
 * @since 3.0.3
 */
@CompileStatic
class JavaCompiler {

    static boolean isAvailable() {
        ClassUtils.isPresent('javax.tools.JavaCompiler', JavaCompiler.classLoader)
    }

    static boolean recompile(CompilerConfiguration config, File... files) {
        // compile java source
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler()
        StandardJavaFileManager sfm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
        javax.tools.JavaCompiler.CompilationTask compileTask = compiler.getTask(null, null, null,
                ['-d', config.targetDirectory.absolutePath], null, sfm.getJavaFileObjects(files))
        compileTask.call()
    }

}
