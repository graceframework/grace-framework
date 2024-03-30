/*
 * Copyright 2021-2022 the original author or authors.
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
package org.grails.plugins.web.mapping;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grails.web")
public class UrlMappingsProperties {

    private final LinkGenerator linkGenerator = new LinkGenerator();

    private final Url url = new Url();

    public LinkGenerator getLinkGenerator() {
        return this.linkGenerator;
    }

    public Url getUrl() {
        return this.url;
    }

    public static class LinkGenerator {

        private boolean useCache = false;

        public boolean isUseCache() {
            return this.useCache;
        }

        public void setUseCache(boolean useCache) {
            this.useCache = useCache;
        }

    }

    public static class Url {

        private String converter = "camelCase";

        public String getConverter() {
            return this.converter;
        }

        public void setConverter(String converter) {
            this.converter = converter;
        }

    }

}
