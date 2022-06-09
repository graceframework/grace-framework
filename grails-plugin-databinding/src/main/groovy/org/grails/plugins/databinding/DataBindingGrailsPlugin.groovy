package org.grails.plugins.databinding

import grails.util.GrailsUtil

/**
 * Keep this plugin when not using micronaut,
 * Use {@link DataBindingConfiguration} instead when using micronaut
 */
class DataBindingGrailsPlugin extends AbstractDataBindingGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()

    @Override
    Closure doWithSpring() {
        return super.doWithSpring()
    }
}
