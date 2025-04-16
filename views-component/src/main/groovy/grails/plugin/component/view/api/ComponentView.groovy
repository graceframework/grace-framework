package grails.plugin.component.view.api

import grails.views.api.GrailsView
import grails.views.api.GrailsViewHelper
import grails.views.api.internal.DefaultGrailsViewHelper

/**
 * Extra methods added to markup views
 *
 * @author Graeme Rocher
 * @since 1.0
 */
trait ComponentView extends GrailsView {

    private GrailsViewHelper viewHelper = new DefaultGrailsViewHelper(this)

    /**
     * @return Obtain the view helper
     */
    GrailsViewHelper getG() {
        return this.viewHelper
    }

}