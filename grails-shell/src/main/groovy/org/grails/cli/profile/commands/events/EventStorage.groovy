/*
 * Copyright 2014-2022 the original author or authors.
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
package org.grails.cli.profile.commands.events

import groovy.transform.CompileStatic

/**
 * Stores command line events
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class EventStorage {

    private static final Map<String, Collection<Closure>> EVENT_LISTENERS = [:].withDefault { [] }

    static void registerEvent(String eventName, Closure callable) {
        if (!EVENT_LISTENERS[eventName].contains(callable)) {
            EVENT_LISTENERS[eventName] << callable
        }
    }

    static void fireEvent(Object caller, String eventName, Object...args) {
        def listeners = EVENT_LISTENERS[eventName]
        for (listener in listeners) {
            listener.delegate = caller
            listener.call args
        }
    }

}
