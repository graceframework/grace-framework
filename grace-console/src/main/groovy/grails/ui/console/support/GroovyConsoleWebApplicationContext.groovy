/*
 * Copyright 2014-2023 the original author or authors.
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
package grails.ui.console.support

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import grails.ui.support.DevelopmentGrailsApplicationContext

/**
 * A {@org.springframework.web.context.WebApplicationContext} for use in the embedded Grails console
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@InheritConstructors
@CompileStatic
class GroovyConsoleWebApplicationContext extends DevelopmentGrailsApplicationContext {

}
