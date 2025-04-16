package grails.views.gradle.component

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
class ComponentViewCompilerTask extends AbstractGroovyTemplateCompileTask {

    @Inject
    ComponentViewCompilerTask(ExecOperations execOperations) {
        super(execOperations)
    }

    @Input
    @Override
    String getFileExtension() {
        "gcom"
    }

    @Input
    @Override
    String getScriptBaseName() {
        "grails.plugin.component.view.ComponentViewTemplate"
    }

    @Input
    @Override
    protected String getCompilerName() {
        "grails.plugin.component.view.ComponentViewCompiler"
    }

}
