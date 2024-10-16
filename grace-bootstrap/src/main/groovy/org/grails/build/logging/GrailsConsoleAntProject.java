/*
 * Copyright 2022-2024 the original author or authors.
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
package org.grails.build.logging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.Project;

/**
 * Extended {@link Project} with Options
 *
 * @author Michael Yan
 * @since 2023.0
 */
public class GrailsConsoleAntProject extends Project {

    private final Map<String, String> options = new HashMap<>();

    public void addOption(String option, String value) {
        this.options.put(option, value);
    }

    public void setOptions(Map<String, String> options) {
        this.options.putAll(options);
    }

    public Map<String, String> getOptions() {
        return this.options;
    }

    public boolean hasFeature(String feature) {
        String features = getProperty("grails.app.features");
        if (features != null && features.contains(",")) {
            return Arrays.asList(features.split(",")).contains(feature);
        }
        else {
            return features != null && features.equals(feature);
        }
    }

    public String getGrailsVersion() {
        return getProperty("grails.version");
    }

    public String getGraceVersion() {
        return getProperty("grace.version");
    }

}
