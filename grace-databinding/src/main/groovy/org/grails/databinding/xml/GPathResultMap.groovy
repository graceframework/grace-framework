/*
 * Copyright 2013-2024 the original author or authors.
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
package org.grails.databinding.xml

import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.Node
import groovy.xml.slurpersupport.NodeChild

/**
 * @author Jeff Brown
 * @since 2.3
 */
class GPathResultMap implements Map {

    protected GPathResult gpath
    protected id

    GPathResultMap(GPathResult gpath) {
        this.gpath = gpath
        this.@id = gpath['@id'].text() ?: null
    }

    @Override
    int size() {
        Set uniqueNames = [] as Set
        gpath.children().each { child ->
            uniqueNames << getPropertyNameForNodeChild(child)
        }
        uniqueNames.size()
    }

    @Override
    boolean containsKey(Object key) {
        if (key == 'id') {
            return this.@id != null || gpath['id'].size()
        }
        gpath[key].size()
    }

    @Override
    Set entrySet() {
        Set entries = [] as Set
        Set uniqueChildNames = [] as Set
        gpath.childNodes().each { childNode ->
            uniqueChildNames << getPropertyNameForNode(childNode)
        }
        uniqueChildNames.each { name ->
            Object value = get(name)
            entries << new AbstractMap.SimpleImmutableEntry(name, value)
        }
        if (this.@id != null) {
            entries << new AbstractMap.SimpleImmutableEntry('id', this.@id)
        }
        entries
    }

    @Override
    Object get(Object key) {
        if (key == 'id' && this.@id) {
            return this.@id
        }

        GPathResult value = gpath.children().findAll { it.name() == key }
        if (value.size() == 0) {
            return null
        }
        if (value.size() > 1) {
            List list = []
            value.iterator().each {
                String theId = it.@id.text()
                if (theId == '') {
                    if (it.children().size() > 0) {
                        GPathResultMap theMap = new GPathResultMap(it)
                        list << theMap
                    }
                    else {
                        list << it.text()
                    }
                }
                else {
                    GPathResultMap theMap = new GPathResultMap(it)
                    list << theMap
                }
            }
            return list
        }
        if (value.children().size() == 0) {
            if (value['@id'].text()) {
                return [id: value['@id'].text()]
            }
            return value.text()
        }
        new GPathResultMap(value)
    }

    @Override
    Set keySet() {
        Set keys = gpath.children().collect {
            getPropertyNameForNodeChild it
        } as Set
        if (this.@id != null) {
            keys << 'id'
        }
        keys
    }

    protected String getPropertyNameForNodeChild(NodeChild child) {
        child.name()
    }

    protected String getPropertyNameForNode(Node node) {
        node.name()
    }

    @Override
    void clear() {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean containsValue(Object value) {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean isEmpty() {
        !size()
    }

    @Override
    Object put(key, value) {
        throw new UnsupportedOperationException()
    }

    @Override
    void putAll(Map m) {
        throw new UnsupportedOperationException()
    }

    @Override
    Object remove(Object key) {
        throw new UnsupportedOperationException()
    }

    @Override
    Collection values() {
        throw new UnsupportedOperationException()
    }

}
