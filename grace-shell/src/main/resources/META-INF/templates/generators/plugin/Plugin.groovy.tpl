package $packageName

import grails.plugins.*

class ${className}GrailsPlugin extends DynamicPlugin {

    def version = "1.0.0-SNAPSHOT"

    // TODO Fill in these fields
    def title = "${className}" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = "Brief summary/description of the plugin."

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    Closure doWithDynamicModules() { {->
            // TODO Implement registering dynamic modules to application (optional)
        }
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

}
