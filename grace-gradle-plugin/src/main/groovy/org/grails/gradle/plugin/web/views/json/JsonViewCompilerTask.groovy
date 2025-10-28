package org.grails.gradle.plugin.web.views.json

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations

import org.grails.gradle.plugin.web.views.AbstractGroovyTemplateCompileTask

/**
 * Concrete implementation that compiles JSON templates
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
@CompileStatic
class JsonViewCompilerTask extends AbstractGroovyTemplateCompileTask {

    @Inject
    JsonViewCompilerTask(ExecOperations execOperations) {
        super(execOperations)
    }

    @Input
    @Override
    String getFileExtension() {
        "gson"
    }

    @Input
    @Override
    String getScriptBaseName() {
        "grails.plugin.json.view.JsonViewTemplate"
    }

    @Input
    @Override
    protected String getCompilerName() {
        "grails.plugin.json.view.JsonViewCompiler"
    }

}
