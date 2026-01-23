/*
 * Copyright 2014-2026 the original author or authors.
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
package org.grails.cli.profile

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

/**
 * Describes a {@link org.grails.cli.command.Command}
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @deprecated since 2024.0.0, in favor of {@link org.grails.cli.command.CommandDescription}
 * @since 3.0
 */
@Canonical
@InheritConstructors
@Deprecated(since = '2024.0.0', forRemoval = true)
class CommandDescription extends org.grails.cli.command.CommandDescription {

}
