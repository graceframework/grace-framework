/*
 * Copyright 2004-2022 the original author or authors.
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

package org.grails.plugins.databinding;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Data binding configuration.
 *
 * @author graemerocher
 * @since 4.0
 */
@ConfigurationProperties("grails.databinding")
public class DataBindingConfigurationProperties {

    private boolean trimStrings = true;

    private boolean convertEmptyStringsToNull = true;

    private int autoGrowCollectionLimit = 256;

    private boolean dateParsingLenient = false;

    private List<String> dateFormats = AbstractDataBindingGrailsPlugin.DEFAULT_DATE_FORMATS;

    public boolean isTrimStrings() {
        return this.trimStrings;
    }

    public void setTrimStrings(boolean trimStrings) {
        this.trimStrings = trimStrings;
    }

    public boolean isConvertEmptyStringsToNull() {
        return this.convertEmptyStringsToNull;
    }

    public void setConvertEmptyStringsToNull(boolean convertEmptyStringsToNull) {
        this.convertEmptyStringsToNull = convertEmptyStringsToNull;
    }

    public int getAutoGrowCollectionLimit() {
        return this.autoGrowCollectionLimit;
    }

    public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
        this.autoGrowCollectionLimit = autoGrowCollectionLimit;
    }

    public boolean isDateParsingLenient() {
        return this.dateParsingLenient;
    }

    public void setDateParsingLenient(boolean dateParsingLenient) {
        this.dateParsingLenient = dateParsingLenient;
    }

    public List<String> getDateFormats() {
        return this.dateFormats;
    }

    public void setDateFormats(List<String> dateFormats) {
        this.dateFormats = dateFormats;
    }

}
