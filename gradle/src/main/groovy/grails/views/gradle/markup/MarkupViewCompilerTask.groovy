package grails.views.gradle.markup

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations

import grails.views.gradle.AbstractGroovyTemplateCompileTask

/**
 * MarkupView compiler task for Gradle
 *
 * @author Graeme Rocher
 * @since 1.0
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
