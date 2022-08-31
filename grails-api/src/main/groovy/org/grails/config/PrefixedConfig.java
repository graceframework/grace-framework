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
package org.grails.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import grails.config.Config;

/**
 * A config that accepts a prefix
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class PrefixedConfig implements Config {

    protected String prefix;

    protected String[] prefixTokens;

    protected Config delegate;

    public PrefixedConfig(String prefix, Config delegate) {
        this.prefix = prefix;
        this.prefixTokens = prefix.split("\\.");
        this.delegate = delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PrefixedConfig entries = (PrefixedConfig) o;

        if (this.delegate != null ? !this.delegate.equals(entries.delegate) : entries.delegate != null) {
            return false;
        }
        if (this.prefix != null ? !this.prefix.equals(entries.prefix) : entries.prefix != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.prefix != null ? this.prefix.hashCode() : 0;
        result = 31 * result + (this.delegate != null ? this.delegate.hashCode() : 0);
        return result;
    }

    @Override
    @Deprecated
    public Map<String, Object> flatten() {
        Map<String, Object> flattened = this.delegate.flatten();
        Map<String, Object> map = new LinkedHashMap<>(flattened.size());
        for (String key : flattened.keySet()) {
            map.put(formulateKey(key), flattened.get(key));
        }
        return map;
    }

    @Override
    public Properties toProperties() {
        Map<String, Object> flattened = flatten();
        Properties properties = new Properties();
        properties.putAll(flattened);
        return properties;
    }

    @Override
    public Object getAt(Object key) {
        return get(key);
    }

    @Override
    public Object navigate(String... path) {
        List<String> tokens = new ArrayList<>();
        tokens.addAll(Arrays.asList(this.prefixTokens));
        tokens.addAll(Arrays.asList(path));
        return this.delegate.navigate(tokens.toArray(new String[0]));
    }

    @Override
    public Iterator<Entry<String, Object>> iterator() {
        return entrySet().iterator();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return containsProperty(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public Object get(Object key) {
        return getProperty(key.toString(), Object.class);
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = this.delegate.keySet();
        Set<String> newKeys = new HashSet<>();
        for (String key : keys) {
            newKeys.add(formulateKey(key));
        }
        return newKeys;
    }

    @Override
    public Collection<Object> values() {
        return this.delegate.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        final Set<Entry<String, Object>> entries = this.delegate.entrySet();
        Set<Entry<String, Object>> newEntries = new HashSet<>();
        for (final Entry<String, Object> entry : entries) {
            newEntries.add(new Entry<String, Object>() {
                @Override
                public String getKey() {
                    return formulateKey(entry.getKey());
                }

                @Override
                public Object getValue() {
                    return entry.getValue();
                }

                @Override
                public Object setValue(Object value) {
                    return entry.setValue(value);
                }
            });
        }

        return newEntries;
    }

    @Override
    public boolean containsProperty(String key) {
        return this.delegate.containsProperty(formulateKey(key));
    }

    @Override
    public String getProperty(String key) {
        return this.delegate.getProperty(formulateKey(key));
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return this.delegate.getProperty(formulateKey(key), defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return this.delegate.getProperty(formulateKey(key), targetType);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return this.delegate.getProperty(formulateKey(key), targetType, defaultValue);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        return this.delegate.getRequiredProperty(formulateKey(key));
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        return this.delegate.getRequiredProperty(formulateKey(key), targetType);
    }

    protected String formulateKey(String key) {
        return this.prefix + '.' + key;
    }

    @Override
    public String resolvePlaceholders(String text) {
        throw new UnsupportedOperationException("Resolving placeholders not supported");
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Resolving placeholders not supported");
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("Config cannot be modified");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("Config cannot be modified");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("Config cannot be modified");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Config cannot be modified");
    }

    @Override
    public Config merge(Map<String, Object> toMerge) {
        throw new UnsupportedOperationException("Config cannot be modified");
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue, List<T> allowedValues) {
        return this.delegate.getProperty(key, targetType, defaultValue, allowedValues);
    }

    @Override
    public void setAt(Object key, Object value) {
        throw new UnsupportedOperationException("Config cannot be modified");
    }

}
