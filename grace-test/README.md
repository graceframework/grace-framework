# Grace Test

The `grace-test` module is the foundational testing infrastructure for Grace applications. It provides mock objects, context loaders, web request utilities, and Spock/JUnit integration that all other test-related modules build upon.


## Module Purpose

`grace-test` is a low-level test support library. It does **not** contain unit test mixins or `@TestFor` annotations (those are in `grace-test-support`). Instead, it provides the core mock objects, context loading, and test utilities that higher-level test modules depend on.


## Important APIs

### 1. `MockGrailsApplication` (`org.grails.plugins.MockGrailsApplication`)

A mock implementation of `GrailsApplication` for use in unit tests. Allows registering artefacts and configuring the application without starting a full Spring context.

### 2. `MockGrailsPluginManager` (`org.grails.plugins.MockGrailsPluginManager`)

A mock implementation of `GrailsPluginManager` for unit tests that need to simulate plugin presence/absence without loading actual plugins.

### 3. `MockApplicationContext` (`org.grails.support.MockApplicationContext`)

A mock Spring `ApplicationContext` that allows registering beans manually for unit tests without starting a real Spring container.

### 4. `GrailsWebMockUtil` (`grails.util.GrailsWebMockUtil`)

A utility class for creating mock `GrailsWebRequest` instances bound to the current thread. Used in controller and taglib unit tests to simulate an HTTP request/response cycle without a real servlet container.

### 5. `GrailsApplicationContextLoader` (`grails.boot.test.GrailsApplicationContextLoader`)

A Spring `SmartContextLoader` that bootstraps a full `GrailsApplication` context for integration tests. Used with `@SpringBootTest` to load the Grace application context in the same way as production.

### 6. `AbstractClosureProxy` / `MockClosureProxy` (`grails.test`)

`AbstractClosureProxy` is an abstract wrapper for Groovy closures that intercepts invocations via `doBeforeCall()` and `doAfterCall()` hooks. `MockClosureProxy` is the concrete implementation used in tests to verify closure invocations.

### 7. `org.grails.test.spock` package

Contains Spock integration classes (likely `GrailsSpockTestTransformer` or similar) that integrate Grace's test lifecycle with Spock specifications.

### 8. `org.grails.plugins.testing` package

Contains testing support for Grace plugins — likely `GrailsMockHttpServletRequest`, `GrailsMockHttpServletResponse`, and related mock servlet objects used in controller/taglib tests.

## How It Works

```
grace-test (foundational mocks + context loader)
  ↓
grace-test-support (unit test mixins: ControllerUnitTest, TagLibUnitTest, etc.)
  ↓
grace-boot-test (bundles grace-test + grace-test-support + spring-boot-starter-test)
  ↓
Application test classes (@GrailsUnitTest, @Integration, etc.)
```

`GrailsApplicationContextLoader` is registered as a `@ContextConfiguration` loader, so `@SpringBootTest` on a Grace integration test automatically bootstraps the full Grace application context. For unit tests, `MockGrailsApplication` + `MockApplicationContext` + `GrailsWebMockUtil` are used to set up a lightweight test environment without Spring.
