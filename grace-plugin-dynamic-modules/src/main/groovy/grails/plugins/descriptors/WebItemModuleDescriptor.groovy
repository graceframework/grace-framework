/*
 * Copyright 2022-2024 the original author or authors.
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
package grails.plugins.descriptors

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.apache.commons.lang3.math.NumberUtils

import grails.plugins.GrailsPlugin
import grails.plugins.exceptions.PluginException

import org.grails.plugins.web.model.WebLabel
import org.grails.plugins.web.model.WebLink

/**
 * WebItemModuleDescriptor
 *
 * @author Michael Yan
 * @since 2022.1.0
 * @deprecated since 2023.0.0, in favor of org.graceframework.plugins:dynamic-modules
 */
@ToString(includeNames=true)
@TupleConstructor
@CompileStatic
@Deprecated(since = "2023.0.0")
class WebItemModuleDescriptor extends AbstractModuleDescriptor {

    String section
    String location
    int weight
    WebLabel webLabel
    WebLink link

    WebItemModuleDescriptor() {
        super()
    }

    void label(Map args) {
        this.webLabel = new WebLabel(args)
    }

    void link(Map args) {
        this.link = new WebLink(args)
    }

    @Override
    void init(GrailsPlugin plugin, Map args) throws PluginException {
        super.init(plugin, args)
        this.section = args.section
        this.location = args.location
        this.weight = NumberUtils.toInt(args.weight as String)
    }

}
