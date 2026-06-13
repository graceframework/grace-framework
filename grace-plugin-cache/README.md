# Grace Plugin Cache

`grace-plugin-cache` is a plugin module that provides powerful and easy-to-use caching functionality for Grace applications and plugins.


### Auto-Configuration

`CacheAutoConfiguration` - The Spring Boot auto-configuration class that registers cache-related beans. It:

- Runs after Spring Boot's `CacheAutoConfiguration` 
- Is conditional on the property `grails.cache.enabled` being true (default)
- Registers `GrailsCacheKeyGenerator` bean (defaults to `SimpleCacheKeyGenerator`)
- Registers a primary `GrailsCacheManager` bean that wraps Spring's `CacheManager`
- Registers `GrailsCacheAdminService` for cache administration

### Cache Manager

`GrailsDelegatingCacheManager` - A delegating implementation of `GrailsCacheManager` that wraps Spring's `CacheManager`. It provides:

- `cacheExists(String name)` - Checks if a cache exists
- `destroyCache(String name)` - Destroys a cache by invoking available removal methods
- `getCache(String name)` - Retrieves a cache by name
- `getCacheNames()` - Returns all cache names

### Plugin Descriptor

`CacheGrailsPlugin` - The Grace plugin descriptor that observes controllers and services, and loads after them. It provides AST transformations for caching method calls.


## Usage in Other Modules

### grace-boot-cache

The `grace-boot-cache` module depends on `grace-plugin-cache` to provide Spring Boot auto-configuration for caching. This follows Grace's pattern of separating core plugin functionality from Spring Boot integration.

### grace-cache-core

The `grace-plugin-cache` module depends on `grace-cache-core` which provides the low-level caching infrastructure including AST transformations and cache-aware traits. The `grace-cache-core` module contains:

- `AbstractCacheTransformation` - Base class for cache annotation transformations
- `GrailsCacheManagerAware` - Trait for classes that need cache manager access
