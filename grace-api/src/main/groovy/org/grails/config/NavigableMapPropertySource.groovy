/*
 * Copyright 2015-2023 the original author or authors.
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
package org.grails.config

import groovy.transform.CompileStatic
import org.springframework.boot.origin.OriginTrackedValue
import org.springframework.core.env.MapPropertySource
import org.springframework.util.StringUtils

/**
 * A {@link org.springframework.core.env.PropertySource} that doesn't return values for navigable submaps
 *
 * This class behavior is closely tied to {@link org.grails.config.NavigableMap} which will be removed in future release.
 * @author Graeme Rocher
 * @since 3.0.7
 */
@CompileStatic
class NavigableMapPropertySource extends MapPropertySource {

    final String[] propertyNames

    final String[] navigablePropertyNames

    NavigableMapPropertySource(String name, NavigableMap source) {
        super(name, source)
        this.propertyNames = source.keySet().findAll({ String key ->
            !(source.get(key) instanceof NavigableMap)
        })?.toArray(new String[0])
        this.navigablePropertyNames = StringUtils.toStringArray(source.keySet())
    }

    @Override
    String[] getPropertyNames() {
        this.propertyNames
    }

    @Override
    Object getProperty(String name) {
        Object value = super.getProperty(name)
        if (value instanceof OriginTrackedValue) {
            return ((OriginTrackedValue) value).value
        }
        else if (value instanceof NavigableMap || value instanceof NavigableMap.NullSafeNavigator) {
            return null
        }
        value
    }

    Object getNavigableProperty(String name) {
        super.getProperty(name)
    }

}
