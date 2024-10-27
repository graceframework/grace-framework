package grails.plugin.cache;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Cache plugin
 *
 * @author Michael Yan
 * @since 6.1
 */
@ConfigurationProperties(value = "grails.cache")
public class CacheProperties {

    public static final String DEFAULT_CACHE_MANAGER = "GrailsConcurrentMapCacheManager";

    /**
     * Enable cache or not.
     */
    private Boolean enabled = true;

    /**
     * Whether to clear caches at startup.
     */
    private Boolean clearAtStartup = false;

    /**
     * Cache manager to use.
     */
    private String cacheManager = DEFAULT_CACHE_MANAGER;

    /**
     * Cache configurations.
     */
    private Map<String, CacheConfig> caches = new HashMap<>();

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getClearAtStartup() {
        return this.clearAtStartup;
    }

    public void setClearAtStartup(Boolean clearAtStartup) {
        this.clearAtStartup = clearAtStartup;
    }

    public String getCacheManager() {
        return this.cacheManager;
    }

    public void setCacheManager(String cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Map<String, CacheConfig> getCaches() {
        return this.caches;
    }

    public void setCaches(Map<String, CacheConfig> caches) {
        this.caches = caches;
    }

    public static class CacheConfig {

        /**
         * Max capacity of the cache.
         */
        private Integer maxCapacity;

        public Integer getMaxCapacity() {
            return this.maxCapacity;
        }

        public void setMaxCapacity(Integer maxCapacity) {
            this.maxCapacity = maxCapacity;
        }
    }

}
