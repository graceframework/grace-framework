/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.boot.devtools;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;

import grails.compiler.ast.ClassInjector;
import grails.util.BuildSettings;
import grails.util.Environment;

import org.grails.boot.internal.JavaCompiler;
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.grails.io.support.GrailsResourceUtils;

/**
 * Grails source compiler
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
public class GrailsSourceCompiler {

    private static final Log logger = LogFactory.getLog(GrailsSourceCompiler.class);

    private static final String SOURCE_MAIN_JAVA = "src/main/java";
    private static final String SOURCE_MAIN_GROOVY = "src/main/groovy";

    GrailsSourceCompiler() {
    }

    public void compile(File changedFile) {
        if (!(changedFile.getName().endsWith(".java") || changedFile.getName().endsWith(".groovy"))) {
            return;
        }
        Environment environment = Environment.getCurrent();
        String location = environment.getReloadLocation();

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setTargetDirectory(new File(location, BuildSettings.BUILD_CLASSES_PATH));

        String grailsAppPath = BuildSettings.GRAILS_APP_PATH;
        String changedPath = changedFile.getPath();

        File appDir = null;
        boolean sourceFileChanged = false;
        for (String dir : Arrays.asList(grailsAppPath, SOURCE_MAIN_JAVA, SOURCE_MAIN_GROOVY)) {
            String changedDir = File.separator + dir;
            if (changedPath.contains(changedDir)) {
                appDir = new File(changedPath.substring(0, changedPath.indexOf(changedDir)));
                sourceFileChanged = true;
                break;
            }
        }

        if (!sourceFileChanged) {
            return;
        }

        String baseFileLocation = appDir.getAbsolutePath();
        compilerConfig.setTargetDirectory(new File(baseFileLocation, BuildSettings.BUILD_CLASSES_PATH));

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Recompiling changed file... [%s]",
                    GrailsResourceUtils.getPathFromBaseDir(changedFile.getAbsolutePath())));
        }
        if (changedFile.getName().endsWith(".java")) {
            if (JavaCompiler.isAvailable()) {
                JavaCompiler.recompile(compilerConfig, changedFile);
            }
            else {
                logger.error(String.format("Cannot recompile [%s], "
                                + "the current JVM is not a JDK (recompilation will not work on a JRE missing the compiler APIs).",
                        changedFile.getName()));
            }
        }
        else {
            compileGroovyFile(compilerConfig, changedFile);
        }
    }

    private void compileGroovyFile(CompilerConfiguration compilerConfig, File changedFile) {
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors();
        for (ClassInjector classInjector : classInjectors) {
            if (classInjector instanceof AbstractGrailsArtefactTransformer) {
                ((AbstractGrailsArtefactTransformer) classInjector).clearCachedState();
            }
        }
        // only one change, just to a simple recompile and propagate the change
        CompilationUnit unit = new CompilationUnit(compilerConfig);
        unit.addSource(changedFile);
        unit.compile();
    }

}
