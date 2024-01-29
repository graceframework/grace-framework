/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.taglib

import groovy.transform.CompileStatic

/**
 * Allows dispatching to namespaced tag libraries and is used within controllers and tag libraries
 * to allow namespaced tags to be invoked as methods (eg. g.link(action:'foo')).
 *
 * @author Michael Yan
 * @since 1.0
 */
@CompileStatic
interface NamespacedTagDispatcher {

    String getNamespace()

    void setTagLibraryLookup(TagLibraryLookup lookup)

    def methodMissing(String name, Object args)

}
