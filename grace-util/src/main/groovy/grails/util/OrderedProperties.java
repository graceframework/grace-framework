/*
 * Copyright 2022-2026 the original author or authors.
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
package grails.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ordered Properties
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
public class OrderedProperties extends Properties {

    private static final Comparator<Object> keyComparator = Comparator.comparing(String::valueOf);

    private static final Comparator<Entry<Object, Object>> entryComparator = Entry.comparingByKey(keyComparator);

    /**
     * Return a sorted enumeration of the keys in this {@link Properties} object.
     * @see #keySet()
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(keySet());
    }

    /**
     * Return a sorted set of the keys in this {@link Properties} object.
     */
    @Override
    public Set<Object> keySet() {
        Set<Object> sortedKeys = new TreeSet<>(keyComparator);
        sortedKeys.addAll(super.keySet());
        return Collections.synchronizedSet(sortedKeys);
    }

    /**
     * Return a sorted set of the entries in this {@link Properties} object.
     */
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        Set<Entry<Object, Object>> sortedEntries = new TreeSet<>(entryComparator);
        sortedEntries.addAll(super.entrySet());
        return Collections.synchronizedSet(sortedEntries);
    }

    /**
     * Writes this property list (key and element pairs) in this
     * {@code Properties} table to the properties file.
     * If the file exists, then it will append data to the end of the file.
     *
     * @param file a properties file.
     * @param comments a description of the property list.
     * @throws IOException if writing this property list to the specified
     *             output stream throws an {@code IOException}.
     */
    public void store(File file, String comments) throws IOException {
        boolean append = file.exists();
        OutputStream out = new FileOutputStream(file, append);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1));
        if (append) {
            writer.newLine();
        }
        super.store(writer, comments);
    }

}
