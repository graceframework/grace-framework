package grails.plugin.component.view.internal

import grails.compiler.traits.TraitInjector
import grails.plugin.component.view.ComponentViewTemplate
import grails.views.compiler.ViewsTransform
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.core.io.support.GrailsFactoriesLoader

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
@InheritConstructors
class ComponentViewsTransform extends ViewsTransform {
    @Override
    protected List<TraitInjector> findTraitInjectors() {
        def injectors = super.findTraitInjectors()

        injectors += GrailsFactoriesLoader.loadFactories(TraitInjector).findAll() { TraitInjector ti ->
            ti.artefactTypes.contains(ComponentViewTemplate.TYPE)
        }
        return injectors
    }
}
