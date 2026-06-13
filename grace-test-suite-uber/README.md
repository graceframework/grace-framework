# Grace Test Suite Uber

`grace-test-suite-uber` is the **largest internal test suite** in the Grace Framework — the "uber" name reflects that it covers the widest range of framework features in a single module. Like all `grace-test-suite-*` modules, it is not published as a user-facing artifact.


## Module Dependencies

Unlike `grace-test-suite-persistence`, this module declares some `api` dependencies (not just `testImplementation`):

| Scope | Key Dependencies |
|-------|------------------|
| `api` | `grace-test-suite-base`, `grace-plugin-interceptors`, `grace-plugin-controllers` |
| `testImplementation` | `grace-boot`, `grace-plugin-gsp`, `grace-plugin-rest`, `grace-plugin-url-mappings`, `grace-plugin-services`, `grace-plugin-codecs`, `grace-plugin-databinding`, `grace-plugin-datasource`, `grace-plugin-i18n`, `grace-test-support`, `grace.datastore.gorm.hibernate` |
| `testRuntimeOnly` | `h2`, `aspectj`, `jakarta.servlet.jsp`, `jakarta.el` |

The `api` scope on `grace-plugin-interceptors` and `grace-plugin-controllers` means `grace-test-suite-uber` is itself a transitive dependency aggregator — `grace-test-suite-web` depends on it and gets those modules for free.


## Source Structure

The module has no `src/main/` sources — only `src/test/groovy/`:

```
src/test/groovy/
├── grails/
│   ├── compiler/          # @GrailsCompileStatic / @GrailsTypeChecked compilation tests
│   ├── persistence/       # @Entity AST transform tests
│   ├── plugins/           # DefaultGrailsPluginManager loading tests
│   ├── spring/            # BeanBuilder DSL tests
│   ├── test/
│   │   ├── mixin/         # ~40 controller/domain/service/GSP/interceptor/URL mapping tests
│   │   └── runtime/       # DirtiesRuntime tests
│   ├── validation/        # Domain constraint getter tests
│   └── web/               # JSONBuilder tests
└── org/grails/
    ├── cli/               # ScriptNameResolver tests
    ├── commons/           # Artefact handler, GrailsClass, plugin manager tests
    ├── domain/            # Domain relationship test fixtures
    ├── plugins/           # DefaultGrailsPlugin, PluginFilter tests
    ├── reload/            # Spring proxied bean reload tests
    ├── support/           # StaticResourceLoader tests
    ├── test/support/      # Test support infrastructure tests
    ├── validation/        # Constraint builder, cascading error tests
    └── web/               # Codecs, errors, filters, i18n, JSON, metaclass, servlet, util tests
```


## What Each Area Tests

### Compiler (`grails/compiler/`)

**`GrailsCompileStaticCompilationErrorsSpec`** and **`GrailsTypeCheckedCompilationErrorsSpec`** — verify that `@GrailsCompileStatic` and `@GrailsTypeChecked` correctly allow valid GORM dynamic finder calls (e.g., `Person.findAllByName(...)`) while rejecting invalid ones at compile time. They use `GroovyClassLoader.parseClass()` to compile code snippets and assert on `MultipleCompilationErrorsException`.


### Entity Transform (`grails/persistence/`)

**`EntityTransformTests`** — uses a `GroovyShell` to evaluate Groovy code snippets with `@Entity` classes and verifies that the AST transformation correctly handles edge cases (regression tests for specific JIRA issues like GRAILS-5238).


### Plugin Manager (`grails/plugins/`)

**`DefaultGrailsPluginManagerTests`** — tests `DefaultGrailsPluginManager.loadPlugins()` with dynamically compiled plugin classes, verifying topological sort, `dependsOn` resolution, and `IncludingPluginFilter` behavior.


### BeanBuilder DSL (`grails/spring/`)

**`BeanBuilderTests`** and **`DynamicElementReaderTests`** — comprehensive tests for the `BeanBuilder` Groovy DSL: importing Spring XML, abstract beans, scoped beans, Spring AOP proxies, `@Component` scanning, `@Lazy`, and more.


### Test Mixin / Testing Traits (`grails/test/mixin/`)

This is the largest area — ~40 test files covering the `grace-test-support` testing traits:

| Test File | What it covers |
|-----------|----------------|
| `ControllerUnitTestMixinTests` | `ControllerUnitTest` trait: request/response mocking, `render`, `redirect`, `params` |
| `DomainClassControllerUnitTestMixinTests` | Controller tests with mocked domain classes |
| `GroovyPageUnitTestMixinTests` | `ViewUnitTest` trait: GSP rendering in unit tests |
| `InterceptorUnitTestMixinSpec` | `InterceptorUnitTest` trait |
| `UrlMappingsTestMixinTests` | `UrlMappingsUnitTest` trait |
| `RestfulControllerSpec` | `RestfulController` base class behavior |
| `ResourceAnnotationRestfulControllerSpec` | `@Resource` annotation on domain classes |
| `ServiceTestMixinInheritanceSpec` | `ServiceUnitTest` trait with inheritance |
| `SetupTeardownInvokeTests` | Lifecycle ordering of `setup`/`cleanup` in test traits |
| `DirtiesRuntimeSpec` | `@DirtiesRuntime` annotation resets the test runtime between tests |
| `SpyBeanSpec` | Mockito `@SpyBean` integration |
| `StaticCallbacksSpec` | `setupSpec`/`cleanupSpec` with test traits |
| `AutowireServiceViaDefineBeansTests` | `defineBeans {}` block in unit tests |
| `MainContextTests` | `mainContext` access in unit tests |


### Validation (`grails/validation/` and `org/grails/validation/`)

**`DomainConstraintGettersSpec`** — tests that constraint discovery correctly handles public/private/static properties, getter-only properties, inherited properties, trait properties, and `transients` declarations.

**`CascadingErrorCountSpec`**, **`ConstrainedPropertyBuilderForCommandsTests`**, **`UrlConstrainedPropertyBuilderForCommandsTests`** — test constraint builder behavior for command objects and URL mappings.


### Artefact Handlers (`org/grails/commons/`)

Tests for every artefact handler: `BootstrapArtefactHandlerTests`, `CodecArtefactHandlerTests`, `ControllerArtefactHandlerTests`, `DomainClassArtefactHandlerTests`, `ServiceArtefactHandlerTests`, `TagLibArtefactHandlerTests`, `UrlMappingsArtefactHandlerTests`. Also `GrailsClassTests` (naming conventions), `DefaultArtefactInfoTests`, `GrailsPluginManagerTests`.


### Web Layer (`org/grails/web/`)

Tests across many web subsystems:

| Subdirectory | What it covers |
|--------------|----------------|
| `codecs/` | Codec encoding/decoding (HTML, URL, JSON, Base64, etc.) |
| `errors/` | Exception handler resolution, error page rendering |
| `filters/` | Filter chain behavior (legacy filter tests) |
| `i18n/` | `ParamsAwareLocaleChangeInterceptor`, locale resolution |
| `json/` | JSON converter, `JSONObject`, `JSONArray` |
| `metaclass/` | Dynamic controller methods (`render`, `redirect`, `chain`, `forward`) |
| `plugins/` | Web plugin bean registration |
| `servlet/` | `GrailsWebRequest`, `RequestContextHolder`, `FlashScope` |
| `util/` | `WebUtils`, `ControllerResultTransformer` |


### Other Areas

- **`org/grails/plugins/`** — `DefaultGrailsPluginTests` (plugin lifecycle, `doWithSpring`, `onChange`), `PluginFilterFactoryTests`
- **`org/grails/reload/`** — `SpringProxiedBeanReloadTests` (verifies that Spring AOP-proxied beans can be reloaded without breaking the proxy)
- **`org/grails/cli/`** — `ScriptNameResolverTests` (CLI script name resolution)
- **`org/grails/support/`** — `StaticResourceLoaderTests`


## Test Isolation

Six tests are excluded from the default `test` task and run in separate Gradle tasks:

| Task | Tests | Reason |
|------|-------|--------|
| `isolatedTestsOne` | `DataSourceGrailsPluginTests`, `GrailsUnitTestCaseTests`, `WebUtilsTests` | Classpath/state conflicts |
| `isolatedTestsTwo` | `UrlMappingsTestMixinTests`, `SetupTeardownInvokeTests`, `TestMixinSetupTeardownInvokeTests` | `maxParallelForks=1` required |
| `isolatedRestRendererTests` | All `rest/render/**/*Spec` | REST renderer isolation |
| `isolatedDefaultGrailsControllerClassTests` | `DefaultGrailsControllerClassTests` | Controller class metadata isolation |
| `isolatedPersonTests` | `PersonTests`, `TestingValidationSpec`, `CascadingErrorCountSpec` | Validation state isolation |
| `isolatedRestfulControllerTests` | `RestfulControllerSpec`, `ResourceAnnotationRestfulControllerSpec` | RESTful controller isolation |

The default `test` task runs with `maxParallelForks = 4` (2 on CI) and forks every 100 tests (25 on CI).


## How It Works

Tests use the same two approaches as `grace-test-suite-persistence`:

1. **`grace-test-support` traits** (`ControllerUnitTest`, `ServiceUnitTest`, `DataTest`, `ViewUnitTest`, `InterceptorUnitTest`, `UrlMappingsUnitTest`) — set up lightweight in-memory Grace contexts for unit testing individual artefacts.

2. **`AbstractGrailsMockTests`** (from `grace-test-suite-base`) — creates a `DefaultGrailsApplication` + `MockApplicationContext` for lower-level tests that need to exercise artefact handlers, plugin loading, or Spring bean registration directly.
