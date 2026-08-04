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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ordered Properties
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
public class OrderedProperties extends Properties {

    private final Map<Object, Object> treeMap = new TreeMap<>();

    @Override
    public Object put(Object key, Object value) {
        this.treeMap.put(key, value);
        return super.put(key, value);
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        this.treeMap.putAll(t);
        super.putAll(t);
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.treeMap.entrySet();
    }

    @Override
    public void clear() {
        this.treeMap.clear();
        super.clear();
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        storeOrdered(this.treeMap, out, comments);
    }

    private void storeOrdered(Map<Object, Object> map, OutputStream out, String comments) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "ISO-8859-1"));
        if (comments != null) {
            writer.write("#" + comments);
            writer.newLine();
        }
        writer.write("#" + new Date());
        writer.newLine();

        synchronized (this) {
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                writer.write(key + "=" + value);
                writer.newLine();
            }
        }
        writer.flush();
    }

}
