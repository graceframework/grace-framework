# Grace Test Suite Web

`grace-test-suite-web` is an **internal test suite** for the Grace Framework's web layer. Like all `grace-test-suite-*` modules, it is not published as a user-facing artifact.


## Module Dependencies

Key dependencies:

| Scope | Key Dependencies |
|-------|------------------|
| `api` | `jakarta.servlet` (exposed transitively) |
| `testImplementation` | `grace-boot`, `grace-test-suite-base`, `grace-plugin-controllers`, `grace-plugin-interceptors`, `grace-plugin-rest`, `grace-plugin-gsp`, `grace-plugin-converters`, `grace-plugin-codecs`, `grace-plugin-url-mappings`, `grace-plugin-databinding`, `grace-plugin-domain-class`, `grace-plugin-datasource`, `grace-plugin-services`, `grace-plugin-i18n`, `grace-web`, `grace-test-support`, `grace.datastore.gorm.hibernate` |
| `testRuntimeOnly` | `jakarta.servlet.jsp`, `jakarta.servlet.jsp.jstl`, `glassfish.web.jsp.jstl`, `aspectj.rt`, `aspectj.weaver` |

The JSP/JSTL runtime dependencies distinguish this module from `grace-test-suite-uber` — it specifically tests JSP tag integration and GSP rendering in a full servlet container context.


## Source Structure

The module has no `src/main/` sources — only `src/test/groovy/`:

```
src/test/groovy/
├── grails/
│   ├── artefact/      # Artefact-related web tests
│   ├── rest/          # REST respond/content negotiation tests
│   └── test/          # Testing trait/mixin tests for web
└── org/grails/        # Framework internals web tests
```


## What It Tests

The isolated test list in `build.gradle` reveals the key areas:

| Area | Test Classes |
|------|--------------|
| Content negotiation | `ContentFormatControllerTests`, `ContentNegotiationSpec`, `WithFormatContentTypeSpec` |
| REST respond method | `RespondMethodSpec` |
| JSON binding | `JSONBindingTests`, `JSONBindingToNullTests` |
| XML binding | `ControllerWithXmlConvertersTests`, `NestedXmlBindingTests` |
| Auto-params marshalling | `AutoParams*MarshallingTests` |
| GSP rendering | `GroovyPageAttributesTests`, `GSPResponseWriterSpec` |
| Data binding | `BindingExcludeTests`, `CommandObjectNoDataSpec` |
| JSP tag integration | `pages/ext/jsp/*` |

The non-isolated tests (run in the default `test` task) cover the broader web layer: controller artefact handling, URL mappings, interceptors, codec encoding in web context, web data binding, and `grace-test-support` traits (`ControllerUnitTest`, `InterceptorUnitTest`, `UrlMappingsUnitTest`, `ViewUnitTest`).


## Test Isolation

The build defines two test tasks:

| Task | Config | Tests |
|------|--------|-------|
| `test` | `maxParallelForks=4` (2 on CI), `forkEvery=100` (10 on CI) | All except isolated list |
| `execIsolatedTests` | `forkEvery=1` (one JVM per test class) | The 12 isolated test classes above |

The `forkEvery=1` for isolated tests is significant — each test class gets its own JVM fork. This is needed because these tests mutate global state (codec registries, content negotiation configuration, GSP page caches, Spring `WebApplicationContext`) that cannot be safely shared between test classes.

The `test` task depends on `execIsolatedTests`, and a `createCombinedReport` task merges both result sets into a single HTML report.


## How It Works

Tests use the same two approaches as the other test suite modules:

1. **`grace-test-support` traits** (`ControllerUnitTest`, `ViewUnitTest`, `InterceptorUnitTest`, `UrlMappingsUnitTest`) — set up lightweight in-memory Grace web contexts for unit testing individual web artefacts without a real servlet container.

2. **`AbstractGrailsMockTests`** (from `grace-test-suite-base`) — creates a `DefaultGrailsApplication` + `MockApplicationContext` for lower-level tests that exercise plugin bean registration, codec configuration, or content negotiation directly.

The key difference from `grace-test-suite-uber` and `grace-test-suite-persistence` is the focus on **web-specific behavior**: GSP rendering pipeline, HTTP content negotiation (`respond` method, `withFormat`), JSON/XML converters in a web context, and JSP tag library integration — all of which require the full `grace-web` + `grace-plugin-gsp` + servlet container stack.
