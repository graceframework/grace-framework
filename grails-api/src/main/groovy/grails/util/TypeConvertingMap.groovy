/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util

import groovy.transform.CompileStatic

/**
 * TypeConvertingMap is a Map with type conversion capabilities.
 *
 * Type converting maps have no inherent ordering. Two maps with identical entries
 * but arranged in a different order internally are considered equal.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
@CompileStatic
class TypeConvertingMap extends AbstractTypeConvertingMap {

    TypeConvertingMap() {
        super()
    }

    TypeConvertingMap(Map map) {
        super(map)
    }

    Object clone() {
        new TypeConvertingMap(new LinkedHashMap(this.wrappedMap))
    }

    Byte 'byte'(String name) {
        getByte(name)
    }

    Byte 'byte'(String name, Integer defaultValue) {
        getByte(name, defaultValue)
    }

    Character 'char'(String name) {
        getChar(name)
    }

    Character 'char'(String name, Character defaultValue) {
        getChar(name, defaultValue?.charValue() as Integer)
    }

    Character 'char'(String name, Integer defaultValue) {
        getChar(name, defaultValue)
    }

    Integer 'int'(String name) {
        getInt(name)
    }

    Integer 'int'(String name, Integer defaultValue) {
        getInt(name, defaultValue)
    }

    Long 'long'(String name) {
        getLong(name)
    }

    Long 'long'(String name, Long defaultValue) {
        getLong(name, defaultValue)
    }

    Short 'short'(String name) {
        getShort(name)
    }

    Short 'short'(String name, Integer defaultValue) {
        getShort(name, defaultValue)
    }

    Double 'double'(String name) {
        getDouble(name)
    }

    Double 'double'(String name, Double defaultValue) {
        getDouble(name, defaultValue)
    }

    Float 'float'(String name) {
        getFloat(name)
    }

    Float 'float'(String name, Float defaultValue) {
        getFloat(name, defaultValue)
    }

    Boolean 'boolean'(String name) {
        getBoolean(name)
    }

    Boolean 'boolean'(String name, Boolean defaultValue) {
        getBoolean(name, defaultValue)
    }

}
