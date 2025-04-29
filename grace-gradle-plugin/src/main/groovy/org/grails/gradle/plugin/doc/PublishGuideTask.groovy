/*
 * Copyright 2014-2025 the original author or authors.
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
package org.grails.gradle.plugin.doc

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.work.InputChanges

import grails.doc.DocPublisher

/**
 * A task used to publish the user guide if a publin that is in GDoc format
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class PublishGuideTask extends AbstractCompile {
    private final static String SOURCE_AND_TARGET_COMPATIBILITY = '17'

    @InputDirectory
    @Optional
    File resourcesDir

    @InputFile
    @Optional
    File propertiesFile

    @InputDirectory
    @Optional
    File groovydocDir

    @InputDirectory
    @Optional
    File javadocDir

    @InputDirectory
    File srcDir

    PublishGuideTask() {
        setSourceCompatibility(SOURCE_AND_TARGET_COMPATIBILITY)
        setTargetCompatibility(SOURCE_AND_TARGET_COMPATIBILITY)
    }

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
    void execute(InputChanges inputs) {
        compile()
    }

    @CompileDynamic
    protected void compile() {
        File destinationDir = getDestinationDirectory().getAsFile().getOrNull()
        DocPublisher docPublisher = new DocPublisher(srcDir, destinationDir, project.logger)

        if (groovydocDir?.exists()) {
            project.copy {
                from groovydocDir
                into "$destinationDir/gapi"
            }
        }
        if (javadocDir?.exists()) {
            project.copy {
                from javadocDir
                into "$destinationDir/api"
            }
        }
        docPublisher.title = project.name
        docPublisher.version = project.version
        docPublisher.src = srcDir
        docPublisher.target = destinationDir
        docPublisher.workDir = new File(project.buildDir, 'doc-tmp')
        docPublisher.apiDir = destinationDir
        if (resourcesDir) {
            docPublisher.images = new File(resourcesDir, 'img')
            docPublisher.css = new File(resourcesDir, 'css')
            docPublisher.js = new File(resourcesDir, 'js')
            docPublisher.style = new File(resourcesDir, 'style')
        }
        if (propertiesFile) {
            docPublisher.propertiesFile = propertiesFile
        }

        docPublisher.publish()
    }

}
