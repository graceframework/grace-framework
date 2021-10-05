package org.grails.gradle.plugin.web.gsp

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.compile.BaseForkOptions
import org.gradle.api.tasks.compile.GroovyForkOptions

/**
* Presents the Compile Options used by the {@llink GroovyPageForkCompileTask}
*
* @author David Estes
* @since 4.0
*/
class GspCompileOptions implements Serializable {
    private static final long serialVersionUID = 0L;

    @Input
	String encoding = "UTF-8"

    @Nested
    GroovyForkOptions forkOptions = new GroovyForkOptions()
}