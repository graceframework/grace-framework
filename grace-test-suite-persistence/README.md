# Grace Test Suite Persistence

`grace-test-suite-persistence` is an **internal test suite** for the Grace Framework itself. Like all `grace-test-suite-*` modules, it is not published as a user-facing artifact — the root build marks it with `isTestSuite = true`.

Its purpose is to verify the correctness of persistence-related framework features: domain class artefact injection, GORM unit test support, data binding with domain objects, transaction management, and datasource plugin configuration.

## What Each Test Covers

### Domain Class Artefact Injection

**`DomainClassTraitSpec`** — verifies that the compile-time AST transformation correctly injects the `DomainClass` trait into classes annotated with either `@Artefact('Domain')` or `@Entity`.

**`DomainPropertiesAccessorSpec`** — verifies that the AST injector adds a binding constructor (`new TestDomain(age: 10)`), a `setProperties()` setter, and a `getProperties()` accessor to `@Entity` classes.


### GORM Unit Test Support (`DataTest` / `DomainUnitTest`)

**`DomainClassUnitTestMixinTests`** — uses the `DataTest` trait from `grace-test-support` to mock domain classes in-memory. Tests `hasMany`/`belongsTo` back-reference assignment and `withTransaction {}`.

**`SaveDomainSpec`** — uses `DomainUnitTest<Person>` to verify that `dateCreated` and `lastUpdated` are auto-populated by GORM on `save()`.

**`WithCriteriaReadOnlySpec`** — verifies that the GORM criteria API (`withCriteria { readOnly true }`) works correctly in a `DomainUnitTest` context.


### Data Binding with Domain Objects

**`GrailsWebDataBinderSpec`** — the largest test file. Uses `DataTest` to mock domain classes and exercises `GrailsWebDataBinder` comprehensively: association binding, collection binding, `@BindUsing`, `@BindingFormat`, blank/null string handling, type conversion errors, and more.

**`GrailsWebDataBinderConfigurationSpec`** — tests `GrailsWebDataBinder` configuration options (e.g., `autoGrowCollectionLimit` for Maps and domain object collections). Kept separate from `GrailsWebDataBinderSpec` because it mutates the binder instance.

**`GrailsWebDataBinderBindingXmlSpec`** — tests binding from `XmlSlurper` parsed XML to domain objects with `hasMany` associations.

**`GrailsWebDataBinderListenerSpec`** — tests that `DataBindingListener.supports()` is respected (a listener registered for `Person` should not fire when binding a `Country`).

**`GrailsWebDataBindingListenerSpec`** — tests that a `DataBindingListenerAdapter` registered as a Spring bean is auto-discovered by `GrailsWebDataBinder`.

**`GrailsWebDataBindingStructuredEditorSpec`** — tests `AbstractStructuredBindingEditor` for binding structured form fields (e.g., `president_firstName`, `president_lastName`) into a custom type.


### Transaction and DataSource Plugin

**`TransactionManagerPostProcessorTests`** — tests `TransactionManagerPostProcessor`, which injects a `PlatformTransactionManager` into any `TransactionManagerAware` bean. Uses `BeanBuilder` with an H2 datasource.

**`DataSourcesGrailsPluginTests`** — extends `AbstractGrailsMockTests` to test `DataSourceGrailsPlugin`'s Spring bean registration for both single and multiple datasource configurations.

**`ScopedProxyAndServiceClassTests`** — regression test for GRAILS-6278. Verifies that `GroovyAwareAspectJAwareAdvisorAutoProxyCreator` correctly handles session-scoped service beans wrapped in `ScopedProxyFactoryBean` (a bug where Groovy's `MetaClass` interfered with AspectJ proxying). 


### Test Fixture

**`MockHibernateGrailsPlugin`** — not a test itself, but a plugin fixture used by other tests. It registers an H2 in-memory datasource and a `DataSourceTransactionManager` via `doWithSpring`.


## Test Isolation

Three tests are excluded from the default `test` task and run in separate Gradle tasks to avoid classpath/state conflicts:

| Task | Tests |
|------|-------|
| `test` (default) | All except the three below |
| `testGrailsDomainBinder` | `GrailsDomainBinderTests` |
| `testIsolatedPersistentOne` | `ComponentValidationTests`, `HibernateMappingUniqueConstraintTests` |

The default `test` task also runs with `maxParallelForks = 2` and forks every 25 tests on CI (100 locally) to manage memory with the H2 database.


## How It Works

The tests use two complementary approaches:

1. **`DataTest` / `DomainUnitTest` traits** (from `grace-test-support`) — call `mockDomains(...)` or `mockDomain(...)` to set up an in-memory GORM session backed by a simple map store. No real database is needed for these tests.

2. **`AbstractGrailsMockTests`** (from `grace-test-suite-base`) — creates a `DefaultGrailsApplication` + `MockApplicationContext`, then manually instantiates plugin classes and calls `doWithRuntimeConfiguration()` to build a real Spring `ApplicationContext` with H2 datasource beans.
