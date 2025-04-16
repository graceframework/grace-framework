package grails.views.gradle

import javax.inject.Inject

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.compile.AbstractOptions
import org.gradle.api.tasks.compile.GroovyForkOptions

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ViewCompileOptions extends AbstractOptions {

    @Input
    String encoding = "UTF-8"

    @Nested
    GroovyForkOptions forkOptions = getObjectFactory().newInstance(GroovyForkOptions.class)

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

}
