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

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges

import grails.doc.DocPublisher
import grails.doc.macros.HiddenMacro

/**
 * A task used to publish the user guide if a publin that is in GDoc format
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class PublishGuideTask extends DefaultTask {

    @InputDirectory
    File sourceDir

    @InputDirectory
    @Optional
    File resourcesDir

    @OutputDirectory
    @Optional
    File targetDir = new File(project.buildDir, 'docs/manual')

    @OutputDirectory
    @Optional
    File workDir = new File(project.buildDir, 'tmp')

    @InputFile
    @Optional
    File propertiesFile

    @InputDirectory
    @Optional
    File groovydocDir

    @InputDirectory
    @Optional
    File javadocDir

    @Input
    @Optional
    Boolean asciidoc = true

    @Input
    @Optional
    Boolean bookmarks = false

    @Input
    @Optional
    String language = ''

    @Input
    @Optional
    String sourceRepo

    @Input
    @Optional
    Collection macros = []

    @Input
    @Optional
    String singleHtml = 'single.html'

    @TaskAction
    void execute(InputChanges inputs) {
        publishGuide()
    }

    protected void publishGuide() {
        DocPublisher docPublisher = new DocPublisher(sourceDir, targetDir, project.logger)

        resourcesDir = resourcesDir ?: new File(sourceDir, 'resources')
        propertiesFile = propertiesFile ?: new File(resourcesDir, 'doc.properties')

        docPublisher.ant = project.ant
        docPublisher.asciidoc = this.asciidoc
        docPublisher.bookmarks = this.bookmarks
        docPublisher.language = this.language
        docPublisher.title = project.name
        docPublisher.version = project.version
        docPublisher.workDir = this.workDir
        docPublisher.apiDir = this.targetDir
        docPublisher.sourceRepo = this.sourceRepo
        docPublisher.images = new File(resourcesDir, 'img')
        docPublisher.css = new File(resourcesDir, 'css')
        docPublisher.js = new File(resourcesDir, 'js')
        docPublisher.style = new File(resourcesDir, 'style')
        docPublisher.propertiesFile = propertiesFile
        docPublisher.singleHtml = this.singleHtml

        // Add custom macros.
        // {hidden} macro for enabling translations.
        docPublisher.registerMacro(new HiddenMacro())

        for (m in macros) {
            docPublisher.registerMacro(m)
        }

        docPublisher.publish()

        if (groovydocDir?.exists()) {
            project.copy {
                from groovydocDir
                into "$targetDir/gapi"
            }
        }
        if (javadocDir?.exists()) {
            project.copy {
                from javadocDir
                into "$targetDir/api"
            }
        }
    }

}
