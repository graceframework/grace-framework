package org.grails.plugins.admin

class AdminUrlMappings {

    static mappings = {
        group "/admin", {
            "/actuator/$action?"(namespace: 'admin', controller: 'actuator')
            "/dashboard/$action?"(namespace: 'admin', controller: 'dashboard')
            "/plugin/$action?"(namespace: 'admin', controller: 'plugin')
            "/system/$action?"(namespace: 'admin', controller: 'system')
        }
    }

}
