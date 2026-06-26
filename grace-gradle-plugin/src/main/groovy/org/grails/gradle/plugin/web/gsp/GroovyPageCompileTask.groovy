/*
 * Copyright 2014-2026 the original author or authors.
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
package org.grails.gradle.plugin.web.gsp

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.work.InputChanges

/**
 * A task for compiling GSPs
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
abstract class GroovyPageCompileTask extends AbstractCompile {

    @Input
    @Optional
    String packagename

    @Input
    @Optional
    String serverpath

    @InputDirectory
    File srcDir

    @InputDirectory
    File configDir

    @Override
    void setSource(Object source) {
        try {
            srcDir = project.file(source)
            if (srcDir.exists() && !srcDir.isDirectory()) {
                throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
            }
            super.setSource(source)
        }
        catch (ignore) {
            throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
        }
    }

    @TaskAction
    @CompileDynamic
    protected void execute(InputChanges inputChanges) {
        def compileTask = this
        Project gradleProject = project
        def antBuilder = gradleProject.services.get(IsolatedAntBuilder)
        String packagename = packagename ?: project.name
        String serverpath = serverpath ?: '/'

        antBuilder.withClasspath(classpath).execute {
            taskdef(name: 'gspc', classname: 'org.grails.web.pages.GroovyPageCompilerTask')
            File dest = compileTask.destinationDirectory.getAsFile().getOrNull()
            File tmpdir = gradleProject.layout.buildDirectory.dir('gsptmp').get().asFile
            dest?.mkdirs()

            gspc(destdir: dest,
                    srcdir: compileTask.srcDir,
                    packagename: packagename,
                    serverpath: serverpath,
                    tmpdir: tmpdir) {
                delegate.configs {
                    pathelement(new File(configDir, 'application.yml').absolutePath)
                    pathelement(new File(configDir, 'application.groovy').absolutePath)
                }
                delegate.classpath {
                    pathelement(path: dest.absolutePath)
                    pathelement(path: compileTask.classpath.asPath)
                }
            }
        }
    }

}
