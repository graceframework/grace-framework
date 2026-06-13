# Grace Boot Cache

`grace-boot-cache` is a Spring Boot auto-configuration module that provides caching functionality for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's Cache functionality more focused and better integrated with Spring Boot's module structure.

The module depends on `grace-plugin-cache` (which contains the core cache functionality), Spring Boot starter, and Spring context support.

### Auto-Configuration

`CacheAutoConfiguration` - Spring Boot auto-configuration that registers the following beans:

- `GrailsCacheKeyGenerator` - Generates cache keys (defaults to `SimpleCacheKeyGenerator`)
- `GrailsCacheManager` - A primary cache manager that delegates to Spring's `CacheManager` or creates a `GrailsConcurrentMapCacheManager` if none is available
- `GrailsCacheAdminService` - Administrative service for cache operations

The configuration is conditional on `grails.cache.enabled=true` (defaults to true) and runs after Spring Boot's `CacheAutoConfiguration`.

### Plugin Definition

`CacheGrailsPlugin` - The Grace plugin that provides AST transformations for caching method calls. It observes controllers and services, and loads after them.
