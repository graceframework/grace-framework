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
package org.grails.config

import java.util.regex.Pattern

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class is deprecated to reduce complexity, improve performance, and increase maintainability.
 * Use {@code config.getProperty(String key, Class<T> targetType)} instead.
 */
@EqualsAndHashCode
@CompileStatic
class NavigableMap implements Map<String, Object>, Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(NavigableMap)

    private static final Pattern SPLIT_PATTERN = ~/\./
    private static final String SPRING_PROFILES = 'spring.profiles.active'
    private static final String SPRING = 'spring'
    private static final String PROFILES = 'profiles'
    private static final String SUBSCRIPT_REGEX = /((.*)\[(\d+)\]).*/

    final NavigableMap rootConfig
    final List<String> path
    final Map<String, Object> delegateMap
    final String dottedPath

    NavigableMap() {
        this.rootConfig = this
        this.path = []
        this.dottedPath = ''
        this.delegateMap = new LinkedHashMap<>()
    }

    NavigableMap(NavigableMap rootConfig, List<String> path) {
        super()
        this.rootConfig = rootConfig
        this.path = path
        this.dottedPath = path.join('.')
        this.delegateMap = new LinkedHashMap<>()
    }

    private NavigableMap(NavigableMap rootConfig, List<String> path, Map<String, Object> delegateMap) {
        this.rootConfig = rootConfig
        this.path = path
        this.dottedPath = path.join('.')
        this.delegateMap = delegateMap
    }

    @Override
    String toString() {
        this.delegateMap.toString()
    }

    @CompileDynamic
    NavigableMap clone() {
        new NavigableMap(this.rootConfig, this.path, this.delegateMap.clone())
    }

    @Override
    int size() {
        this.delegateMap.size()
    }

    @Override
    boolean isEmpty() {
        this.delegateMap.isEmpty()
    }

    @Override
    boolean containsKey(Object key) {
        this.delegateMap.containsKey key
    }

    @Override
    boolean containsValue(Object value) {
        this.delegateMap.containsValue value
    }

    @CompileDynamic
    @Override
    Object get(Object key) {
        Object result = this.delegateMap.get(key)
        if (result != null) {
            return result
        }
        null
    }

    @Override
    Object put(String key, Object value) {
        this.delegateMap.put(key, value)
    }

    @Override
    Object remove(Object key) {
        this.delegateMap.remove key
    }

    @Override
    void putAll(Map<? extends String, ? extends Object> m) {
        this.delegateMap.putAll m
    }

    @Override
    void clear() {
        this.delegateMap.clear()
    }

    @Override
    Set<String> keySet() {
        this.delegateMap.keySet()
    }

    @Override
    Collection<Object> values() {
        this.delegateMap.values()
    }

    @Override
    Set<Map.Entry<String, Object>> entrySet() {
        this.delegateMap.entrySet()
    }

    void merge(Map sourceMap, boolean parseFlatKeys = false) {
        mergeMaps(this, '', this, sourceMap, parseFlatKeys)
    }

    private void mergeMaps(NavigableMap rootMap,
                           String path,
                           NavigableMap targetMap,
                           Map sourceMap,
                           boolean parseFlatKeys) {
        for (Entry entry in sourceMap) {
            Object sourceKeyObject = entry.key
            Object sourceValue = entry.value
            String sourceKey = String.valueOf(sourceKeyObject)
            if (parseFlatKeys) {
                String[] keyParts = sourceKey.split(/\./)
                if (keyParts.length > 1) {
                    mergeMapEntry(rootMap, path, targetMap, sourceKey, sourceValue, parseFlatKeys)
                    List<String> pathParts = keyParts[0..-2]
                    Map actualTarget = targetMap.navigateSubMap(pathParts as List, true)
                    sourceKey = keyParts[-1]
                    mergeMapEntry(rootMap, pathParts.join('.'), actualTarget, sourceKey, sourceValue, parseFlatKeys)
                }
                else {
                    mergeMapEntry(rootMap, path, targetMap, sourceKey, sourceValue, parseFlatKeys)
                }
            }
            else {
                mergeMapEntry(rootMap, path, targetMap, sourceKey, sourceValue, parseFlatKeys)
            }
        }
    }

    private boolean shouldSkipBlock(Map sourceMap, String path) {
        Object springProfileDefined = System.properties.getProperty(SPRING_PROFILES)
        boolean hasSpringProfiles =
                sourceMap.get(SPRING) instanceof Map && ((Map) sourceMap.get(SPRING)).get(PROFILES) ||
                        path == SPRING && sourceMap.get(PROFILES)

        !springProfileDefined && hasSpringProfiles
    }

    protected void mergeMapEntry(NavigableMap rootMap, String path, NavigableMap targetMap, String sourceKey, Object sourceValue,
                                 boolean parseFlatKeys, boolean isNestedSet = false) {
        int subscriptStart = sourceKey.indexOf('[')
        int subscriptEnd = sourceKey.indexOf(']')
        if (subscriptEnd > subscriptStart) {
            if (subscriptStart > -1) {
                String k = sourceKey[0..<subscriptStart]
                String index = sourceKey[subscriptStart + 1..<subscriptEnd]
                String remainder = subscriptEnd != sourceKey.length() - 1 ? sourceKey[subscriptEnd + 2..-1] : null
                if (remainder) {
                    boolean isNumber = index.isNumber()
                    if (isNumber) {
                        int i = index.toInteger()
                        Object currentValue = targetMap.get(k)
                        List list = currentValue instanceof List ? currentValue : []
                        if (list.size() > i) {
                            Object v = list.get(i)
                            if (v instanceof Map) {
                                ((Map) v).put(remainder, sourceValue)
                            }
                            else {
                                Map newMap = [:]
                                newMap.put(remainder, sourceValue)
                                fill(list, i, null)
                                list.set(i, newMap)
                            }
                        }
                        else {
                            Map newMap = [:]
                            newMap.put(remainder, sourceValue)
                            fill(list, i, null)
                            list.set(i, newMap)
                        }
                        targetMap.put(k, list)
                    }
                    else {
                        Object currentValue = targetMap.get(k)
                        Map nestedMap = currentValue instanceof Map ? currentValue : [:]
                        targetMap.put(k, nestedMap)

                        Object v = nestedMap.get(index)
                        if (v instanceof Map) {
                            ((Map) v).put(remainder, sourceValue)
                        }
                        else {
                            Map newMap = [:]
                            newMap.put(remainder, sourceValue)
                            nestedMap.put(index, newMap)
                        }
                    }
                }
                else {
                    Object currentValue = targetMap.get(k)
                    if (index.isNumber()) {
                        List list = currentValue instanceof List ? currentValue : []
                        int i = index.toInteger()
                        fill(list, i, null)
                        list.set(i, sourceValue)
                        targetMap.put(k, list)
                    }
                    else {
                        Map nestedMap = currentValue instanceof Map ? currentValue : [:]
                        targetMap.put(k, nestedMap)
                        nestedMap.put(index, sourceValue)
                    }
                    targetMap.put(sourceKey, sourceValue)
                }
            }
        }
        else {
            Object currentValue = targetMap.containsKey(sourceKey) ? targetMap.get(sourceKey) : null
            Object newValue
            if (sourceValue instanceof Map) {
                List<String> newPathList = []
                newPathList.addAll(targetMap.getPath())
                newPathList.add(sourceKey)
                NavigableMap subMap
                if (currentValue instanceof NavigableMap) {
                    subMap = (NavigableMap) currentValue
                }
                else {
                    subMap = new NavigableMap((NavigableMap) targetMap.getRootConfig(), newPathList.asImmutable())
                    if (currentValue instanceof Map) {
                        subMap.putAll((Map) currentValue)
                    }
                }
                String newPath = path ? "${path}.${sourceKey}" : sourceKey
                mergeMaps(rootMap, newPath, subMap, (Map) sourceValue, parseFlatKeys)
                newValue = subMap
            }
            else {
                newValue = sourceValue
            }
            if (isNestedSet && newValue == null) {
                if (path) {
                    Object subMap = rootMap.get(path)
                    if (subMap instanceof Map) {
                        subMap.remove(sourceKey)
                    }
                    Set<String> keysToRemove = rootMap.keySet().findAll { String key ->
                        key.startsWith("${path}.")
                    }
                    for (key in keysToRemove) {
                        rootMap.remove(key)
                    }
                }
                targetMap.remove(sourceKey)
            }
            else {
                if (path) {
                    rootMap.put("${path}.${sourceKey}".toString(), newValue)
                }
                mergeMapEntry(targetMap, sourceKey, newValue)
            }
        }
    }

    protected Object mergeMapEntry(NavigableMap targetMap, String sourceKey, newValue) {
        targetMap.put(sourceKey, newValue)
    }

    Object getAt(Object key) {
        getProperty(String.valueOf(key))
    }

    void setAt(Object key, Object value) {
        setProperty(String.valueOf(key), value)
    }

    Object getProperty(String name) {
        if (!containsKey(name)) {
            return new NullSafeNavigator(this, [name].asImmutable())
        }
        Object result = get(name)
        result
    }

    void setProperty(String name, Object value) {
        mergeMapEntry(this.rootConfig, this.dottedPath, this, name, value, false, true)
    }

    Object navigate(String... path) {
        navigateMap(this, path)
    }

    private Object navigateMap(Map<String, Object> map, String... path) {
        if (map == null || path == null) {
            return null
        }
        if (path.length == 0) {
            return map
        }
        else if (path.length == 1) {
            return map.get(path[0])
        }
        Object submap = map.get(path[0])
        if (submap instanceof Map) {
            return navigateMap((Map<String, Object>) submap, path.tail())
        }
        submap
    }

    private void fill(List list, Integer toIndex, Object value) {
        if (toIndex >= list.size()) {
            for (int i = list.size(); i <= toIndex; i++) {
                list.add(i, value)
            }
        }
    }

    NavigableMap navigateSubMap(List<String> path, boolean createMissing) {
        NavigableMap rootMap = this
        NavigableMap currentMap = this
        StringBuilder accumulatedPath = new StringBuilder()
        boolean isFirst = true
        for (String pathElement : path) {
            if (isFirst) {
                isFirst = false
                accumulatedPath.append(pathElement)
            }
            else {
                accumulatedPath.append('.').append(pathElement)
            }

            Object currentItem = currentMap.get(pathElement)
            if (currentItem instanceof NavigableMap) {
                currentMap = (NavigableMap) currentItem
            }
            else if (createMissing) {
                List<String> newPathList = []
                newPathList.addAll(currentMap.getPath())
                newPathList.add(pathElement)

                Map<String, Object> newMap = new NavigableMap((NavigableMap) currentMap.getRootConfig(), newPathList.asImmutable())
                currentMap.put(pathElement, newMap)

                String fullPath = accumulatedPath
                if (!rootMap.containsKey(fullPath)) {
                    rootMap.put(fullPath, newMap)
                }
                currentMap = newMap
            }
            else {
                return null
            }
        }
        currentMap
    }

    Map<String, Object> toFlatConfig() {
        Map<String, Object> flatConfig = [:]
        flattenKeys(flatConfig, this, [], false)
        flatConfig
    }

    Properties toProperties() {
        Properties properties = new Properties()
        flattenKeys(properties as Map<String, Object>, this, [], true)
        properties
    }

    private void flattenKeys(Map<String, Object> flatConfig, Map currentMap, List<String> path, boolean forceStrings) {
        currentMap.each { key, value ->
            String stringKey = String.valueOf(key)
            if (value != null) {
                if (value instanceof Map) {
                    List<String> newPathList = []
                    newPathList.addAll(path)
                    newPathList.add(stringKey)

                    flattenKeys(flatConfig, (Map) value, newPathList.asImmutable(), forceStrings)
                }
                else {
                    String fullKey
                    if (path) {
                        fullKey = path.join('.') + '.' + stringKey
                    }
                    else {
                        fullKey = stringKey
                    }
                    if (value instanceof Collection) {
                        if (forceStrings) {
                            flatConfig.put(fullKey, ((Collection) value).join(','))
                        }
                        else {
                            flatConfig.put(fullKey, value)
                        }
                        int index = 0
                        for (Object item : (Collection) value) {
                            String collectionKey = "${fullKey}[${index}]"
                            flatConfig.put(collectionKey, forceStrings ? String.valueOf(item) : item)
                            index++
                        }
                    }
                    else {
                        flatConfig.put(fullKey, forceStrings ? String.valueOf(value) : value)
                    }
                }
            }
        }
    }

    @Override
    int hashCode() {
        this.delegateMap.hashCode()
    }

    @Override
    boolean equals(Object obj) {
        this.delegateMap.equals(obj)
    }

    /**
     * This class will be removed in future.
     * Use {@code config.getProperty(String key, Class<T> targetType)} instead of dot based navigation.
     */
    @CompileStatic
    static class NullSafeNavigator implements Map<String, Object> {

        final NavigableMap parent
        final List<String> path

        NullSafeNavigator(NavigableMap parent, List<String> path) {
            this.parent = parent
            this.path = path
        }

        Object getAt(Object key) {
            getProperty(String.valueOf(key))
        }

        void setAt(Object key, Object value) {
            setProperty(String.valueOf(key), value)
        }

        @Override
        int size() {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.size()
            }
            0
        }

        @Override
        boolean isEmpty() {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.isEmpty()
            }
            true
        }

        boolean containsKey(Object key) {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.containsKey(key)
            }
            false
        }

        @Override
        boolean containsValue(Object value) {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.containsValue(value)
            }
            false
        }

        @Override
        Object get(Object key) {
            getAt(key)
        }

        @Override
        Object put(String key, Object value) {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        Object remove(Object key) {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        void clear() {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        Set<String> keySet() {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.keySet()
            }
            Collections.emptySet()
        }

        @Override
        Collection<Object> values() {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.values()
            }
            Collections.emptySet()
        }

        @Override
        Set<Map.Entry<String, Object>> entrySet() {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.entrySet()
            }
            Collections.emptySet()
        }

        Object getProperty(String name) {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, false)
            if (parentMap != null) {
                return parentMap.get(name)
            }
            new NullSafeNavigator(this.parent, ((this.path + [name]) as List<String>).asImmutable())
        }

        void setProperty(String name, Object value) {
            NavigableMap parentMap = this.parent.navigateSubMap(this.path, true)
            parentMap.setProperty(name, value)
        }

        boolean asBoolean() {
            false
        }

        Object invokeMethod(String name, Object args) {
            throw new NullPointerException("Cannot invoke method $name() on NullSafeNavigator")
        }

        boolean equals(Object to) {
            to == null || DefaultGroovyMethods.is(this, to)
        }

        Iterator iterator() {
            Collections.emptyList().iterator()
        }

        Object plus(String s) {
            toString() + s
        }

        Object plus(Object o) {
            throw new NullPointerException('Cannot invoke method plus on NullSafeNavigator')
        }

        boolean is(Object other) {
            other == null || DefaultGroovyMethods.is(this, other)
        }

        Object asType(Class c) {
            if (c == Boolean || c == boolean) {
                return false
            }
            null
        }

        String toString() {
            null
        }

//        public int hashCode() {
//            throw new NullPointerException("Cannot invoke method hashCode() on NullSafeNavigator");
//        }

    }

}
