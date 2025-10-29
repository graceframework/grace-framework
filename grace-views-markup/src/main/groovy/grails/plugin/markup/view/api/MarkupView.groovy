/*
 * Copyright 2015-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.markup.view.api

import grails.views.api.GrailsView
import grails.views.api.GrailsViewHelper
import grails.views.api.internal.DefaultGrailsViewHelper

/**
 * Extra methods added to markup views
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
trait MarkupView extends GrailsView {

    private GrailsViewHelper viewHelper = new DefaultGrailsViewHelper(this)

    /**
     * @return Obtain the view helper
     */
    GrailsViewHelper getG() {
        return this.viewHelper
    }

}
