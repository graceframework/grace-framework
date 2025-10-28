package org.grails.gradle.plugin.web.views.markup

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations

import org.grails.gradle.plugin.web.views.AbstractGroovyTemplateCompileTask

/**
 * MarkupView compiler task for Gradle
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
@CompileStatic
class MarkupViewCompilerTask extends AbstractGroovyTemplateCompileTask {

    @Inject
    MarkupViewCompilerTask(ExecOperations execOperations) {
        super(execOperations)
    }

    @Input
    @Override
    String getFileExtension() {
        "gml"
    }

    @Input
    @Override
    String getScriptBaseName() {
        "grails.plugin.markup.view.MarkupViewTemplate"
    }

    @Input
    @Override
    protected String getCompilerName() {
        "grails.plugin.markup.view.MarkupViewCompiler"
    }

}
