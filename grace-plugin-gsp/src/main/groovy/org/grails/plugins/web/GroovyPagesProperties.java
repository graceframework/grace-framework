/*
 * Copyright 2024 the original author or authors.
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
package org.grails.plugins.web;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Groovy Pages.
 *
 * @author Michael Yan
 * @since 2023.1.0
 */
@ConfigurationProperties("grails.views.gsp")
public class GroovyPagesProperties {

    /**
     * The encoding to use for GSP views, defaults to UTF-8.
     */
    private String encoding = "UTF-8";

    /**
     * Thew views directory for GSP.
     */
    private String dir;

    /**
     * Comma-separated list of tld patterns to scan JSP taglibs.
     */
    private List<String> tldScanPatterns = List.of("classpath*:/META-INF/*.tld");

    /**
     * Whether to include the jsessionid in the rendered links.
     */
    private boolean includeJsessionid = false;

    private final Reload reload = new Reload();

    private final Layout layout = new Layout();

    private final Cache cache = new Cache();

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getDir() {
        return this.dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public List<String> getTldScanPatterns() {
        return this.tldScanPatterns;
    }

    public void setTldScanPatterns(List<String> tldScanPatterns) {
        this.tldScanPatterns = tldScanPatterns;
    }

    public boolean isIncludeJsessionid() {
        return this.includeJsessionid;
    }

    public void setIncludeJsessionid(boolean includeJsessionid) {
        this.includeJsessionid = includeJsessionid;
    }

    public Reload getReload() {
        return this.reload;
    }

    public Layout getLayout() {
        return this.layout;
    }

    public Cache getCache() {
        return this.cache;
    }

    public static class Reload {

        /**
         * Whether to enable GSP reloading in production.
         */
        private boolean enabled = false;

        /**
         * Checks if the GSP has expired and should be reloaded, default value is 5000 (ms).
         */
        private Integer interval = 5000;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getInterval() {
            return this.interval;
        }

        public void setInterval(Integer interval) {
            this.interval = interval;
        }

    }

    public static class Layout {

        /**
         * Whether to enable layout for GSP.
         */
        private boolean enabled = true;

        /**
         * The default layout for GSP.
         */
        private String defaultName = "application";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultName() {
            return this.defaultName;
        }

        public void setDefaultName(String defaultName) {
            this.defaultName = defaultName;
        }

    }

    public static class Cache {

        /**
         * Whether to enable caching of resources in GSP.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
