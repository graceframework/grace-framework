# Grace Cache Core

`grace-cache-core` is a foundational module that provides the core caching infrastructure for Grace applications, including AST transformations for cache annotations and cache-aware traits.

> [!NOTE]
> It was created during the 2024.x refactoring when the grace-cache plugin was merged into the framework.

### Cache-Aware Trait

`GrailsCacheManagerAware` - A trait for classes that need cache manager access, providing properties for `GrailsCacheManager` and `GrailsCacheKeyGenerator`. This trait is automatically applied to classes using cache annotations.

### AST Transformations

* `AbstractCacheTransformation` - Abstract base class for cache annotation transformations, handling common logic like cache key generation, parameter map creation, and cache variable declaration. It weaves the `GrailsCacheManagerAware` trait into annotated classes.

* `CacheableTransformation` - AST transformation for the `@Cacheable` annotation, implementing method result caching logic.

* `CachePutTransformation` - AST transformation for the `@CachePut` annotation, implementing cache update logic.

* `CacheEvictTransformation` - AST transformation for the `@CacheEvict` annotation, implementing cache eviction logic.


The `grace-plugin-cache` module depends directly on `grace-cache-core` to access the AST transformations and cache-aware traits. The `grace-boot-cache` module then depends on `grace-plugin-cache` to provide Spring Boot auto-configuration for caching.
