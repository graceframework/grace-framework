# Grace Test Suite Base

`grace-test-suite-base` is a **minimal shared test infrastructure library** used exclusively within the Grace Framework's own internal test suites. It is not published as a user-facing artifact — the root `build.gradle` marks all `grace-test-suite-*` projects with `isTestSuite = true` and excludes them from publication.


## Module Dependencies

All dependencies are declared as `api` (transitive), so every module that depends on `grace-test-suite-base` automatically gets `grace-core`, `grace-web`, `grace-test`, `grace-plugin-domain-class`, etc. on its test classpath — which is the main reason this module exists.


## Source Files

The module contains exactly **5 classes** in `src/main/groovy/`:

```
grace-test-suite-base/src/main/groovy/
├── grails/util/
│   ├── MockHttpServletResponse.java
│   └── MockRequestDataValueProcessor.java
└── org/grails/
    ├── commons/test/
    │   └── AbstractGrailsMockTests.java
    └── web/servlet/mvc/
        ├── HibernateProxy.java
        ├── LazyInitializer.java
        └── MockHibernateProxyHandler.java
```


## Key Classes

### 1. `AbstractGrailsMockTests`

The most important class. It is a base class for JUnit/Groovy test cases (extends `GroovyTestCase`) that need a fully initialized `DefaultGrailsApplication` and a `MockApplicationContext` without starting a full Spring Boot context.

Its `setUp()` lifecycle:

1. Enables `ExpandoMetaClass` globally (required for Groovy dynamic method injection)
2. Creates a `MockApplicationContext` and registers a `GroovyClassLoader` bean
3. Calls `onSetUp()` — the hook where subclasses load Groovy source strings into the `gcl` class loader
4. Constructs a `DefaultGrailsApplication` from all classes loaded into `gcl`
5. Optionally parses a `Config` class if one was loaded
6. Calls `grailsApplication.initialise()` to run artefact discovery
7. Calls `postSetUp()` — a second hook for post-initialization setup

Its `tearDown()` disables `ExpandoMetaClass` globally and calls `onTearDown()`.


### 2. `MockHttpServletResponse`

A trivial subclass of Spring's `MockHttpServletResponse` with no added behavior. Its sole purpose is to suppress compiler warnings about deprecated Servlet API methods — by subclassing and re-declaring them as `@Deprecated`, the compiler treats usages as intentional.


### 3. `MockRequestDataValueProcessor`

A test implementation of Spring MVC's `RequestDataValueProcessor` interface. It is used in GSP/tag-lib tests to verify that the framework correctly calls the processor when rendering forms and URLs. Its behavior is deterministic and assertable:

- `processUrl()` appends `?requestDataValueProcessorParamName=paramValue` to every URL
- `processFormFieldValue()` appends `_PROCESSED_` to every field value
- `processAction()` strips the appended param back out (round-trip test)
- `getExtraHiddenFields()` returns a fixed hidden field `requestDataValueProcessorHiddenName=hiddenValue`


### 4. `HibernateProxy` / `LazyInitializer` / `MockHibernateProxyHandler`

These three classes exist to allow tests that exercise Hibernate proxy behavior **without requiring Hibernate on the compile classpath**.

- `HibernateProxy` — a minimal interface mirroring `org.hibernate.proxy.HibernateProxy`, with just `writeReplace()` and `getHibernateLazyInitializer()`
- `LazyInitializer` — a minimal interface mirroring `org.hibernate.proxy.LazyInitializer`, with just `getImplementation()`
- `MockHibernateProxyHandler` — a no-op implementation of `EntityProxyHandler` (from `grace-api`) that always returns `false`/`null` for all proxy checks. Used in tests that need an `EntityProxyHandler` bean registered but don't actually need proxy unwrapping.


## How It Works

The module itself has no runtime logic — it is a **compile-time dependency aggregator** combined with a small set of shared test utilities. Its value is twofold:

1. **Classpath aggregation**: By declaring all core Grace modules as `api` dependencies, any test suite that depends on `grace-test-suite-base` automatically gets the full set of framework modules on its test classpath without having to list them individually.

2. **Shared test infrastructure**: The four test utility classes (`AbstractGrailsMockTests`, `MockHttpServletResponse`, `MockRequestDataValueProcessor`, `MockHibernateProxyHandler`) are used across multiple test suite modules, avoiding duplication.


## How It Is Used in Other Modules

| Module | Dependency scope | What it uses |
|--------|------------------|--------------|
| `grace-test-suite-uber` | `api` | `AbstractGrailsMockTests`, `MockHibernateProxyHandler`, `MockRequestDataValueProcessor`, classpath aggregation |
| `grace-test-suite-web` | `testImplementation` | `MockRequestDataValueProcessor`, `MockHttpServletResponse`, classpath aggregation |
| `grace-test-suite-persistence` | `testImplementation` | `AbstractGrailsMockTests`, classpath aggregation |
| `grace-plugin-rest` | `testImplementation` | `MockHibernateProxyHandler`, classpath aggregation |
| `grace-web-url-mappings` | `testImplementation` | classpath aggregation |
