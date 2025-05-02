/*
 * Copyright 2004-2025 the original author or authors.
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
import org.gradle.api.tasks.TaskAction

import grails.doc.PdfPublisher

/**
 * Gradle task for generating a gdoc-based PDF user guide. Assumes the
 * single page HTML user guide has already been created in the default
 * location.
 */
@CompileStatic
class PublishPdf extends DefaultTask {

    @Input String pdfName = 'single.pdf'
    @Input String language = ''

    @TaskAction
    def publish() {
        File outputDir = project.layout.buildDirectory.get().asFile
        try {
            PdfPublisher.publishPdfFromHtml(outputDir, 'docs/manual/' + language + '/guide/single.html', pdfName)
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }
    }

}
