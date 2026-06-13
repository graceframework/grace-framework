# Grace Views Core

`grace-views-core` is the foundational module for Grace's Groovy-based view technologies. It provides the shared infrastructure for template resolution, compilation, caching, and Spring MVC integration that both `grace-views-json` (JSON views) and `grace-views-markup` (Markup views) build upon.


## Important APIs

### 1. `ViewConfiguration` interface (`grails.views.ViewConfiguration`)

The central configuration contract for a view module. Defines all tunable properties:

| Method | Purpose |
|--------|---------|
| `getExtension()` | File extension (e.g. `gson`, `gml`) |
| `getBaseTemplateClass()` | The Groovy script base class for compiled views |
| `isCompileStatic()` | Whether to use `@CompileStatic` |
| `isEnableReloading()` | Hot-reload in development mode |
| `isPrettyPrint()` | Pretty-print output |
| `isCache()` | Cache resolved templates |
| `getPackageImports()` / `getStaticImports()` | Auto-imports added to every view |
| `getViewModuleName()` | Module name (e.g. `json`, `markup`) |

`GenericViewConfiguration` is the default trait implementation, with sensible defaults (UTF-8 encoding, `compileStatic = true`, cache disabled in dev mode).


### 2. `WritableScript` interface + `AbstractWritableScript`

`WritableScript` is the interface every compiled view implements — it extends both `Writable` (can write itself to a `Writer`) and `WriterProvider`.

`AbstractWritableScript` is the abstract base class that all view scripts extend. It holds the `out` writer, `sourceFile`, and implements `writeTo(Writer)` with development-mode error reporting.


### 3. `GrailsView` trait (`grails.views.api.GrailsView`)

The main trait injected into all view scripts. Extends `HttpView`, `WriterProvider`, and `WritableScript`. Provides access to:

- `LinkGenerator getLinkGenerator()` — URL generation
- `MimeUtility getMimeUtility()` — MIME type resolution
- `MappingContext getMappingContext()` — GORM domain model introspection
- `MessageSource getMessageSource()` — i18n
- `ProxyHandler getProxyHandler()` — GORM proxy unwrapping
- `ResolvableGroovyTemplateEngine getTemplateEngine()` — for rendering sub-templates


### 4. `WritableScriptTemplate` + `GrailsViewTemplate`

`WritableScriptTemplate` implements Groovy's `Template` interface. It wraps a compiled view class and creates `WritableScript` instances via `make(Map binding)`, injecting typed model variables using reflection.

`GrailsViewTemplate` extends it with Grace-specific services (`LinkGenerator`, `MimeUtility`, `MappingContext`, `MessageSource`, `ProxyHandler`, `TemplateEngine`) that are injected by the engine and then accessible from within the view script.


### 5. `ResolvableGroovyTemplateEngine` (abstract)

The core template engine. Extends Groovy's `TemplateEngine` and adds:

- **Locale + qualifier-aware resolution**: resolves `view_en_json.gson` before `view_en.gson` before `view.gson`, supporting MIME type and API version qualifiers
- **Dual resolution strategy**: in dev mode tries file first, in production tries pre-compiled class first
- **Caffeine caching**: two caches — `cachedTemplates` (by path) and `resolveCache` (by path+locale+qualifiers)
- **`ViewsTransform` AST**: applied to every compiled view via `ASTTransformationCustomizer`

Subclasses must implement `getDynamicTemplatePrefix()`.


### 6. `TemplateResolver` interface

Resolves a template by path to either a `URL` (source file) or a pre-compiled `Class`.

`GenericGroovyTemplateResolver` is the default implementation (classpath + filesystem). `PluginAwareTemplateResolver` adds plugin-scoped resolution. 


### 7. `ViewUriResolver` interface

Resolves template URIs using Grace conventions (e.g. `resolveTemplateUri('foo', 'bar')` → `/foo/_bar.gson`).


### 8. Spring MVC Integration (`grails.views.mvc`)

| Class | Role |
|-------|------|
| `SmartViewResolver` | Spring `ViewResolver` with MIME type + version qualifier awareness and Caffeine view cache (max 150 entries). Resolves by `Class` type or path string. |
| `GenericGroovyTemplateViewResolver` | Wraps `SmartViewResolver`, adds controller namespace prefix support for view name resolution. |
| `GenericGroovyTemplateView` | Spring `AbstractUrlBasedView` that calls `template.make(model).writeTo(response.writer)`, sets locale, controller name, action name, request/response on the view. |


### 9. `AbstractGroovyTemplateCompiler`

Used at **build time** (Gradle task) to pre-compile view files to `.class` files. Uses a parallel thread pool (`availableProcessors * 2` threads) and applies the same `ViewsTransform` AST and import customizers as the runtime engine.


## How It Works

```
View file (e.g. book/show.gson)
  ↓ (build time: AbstractGroovyTemplateCompiler)
  ↓ (runtime: ResolvableGroovyTemplateEngine.createTemplate(URL))
Compiled class extends AbstractWritableScript + GrailsView
  ↓
GrailsViewTemplate (wraps class, holds services)
  ↓ (SmartViewResolver / GenericGroovyTemplateViewResolver)
GenericGroovyTemplateView (Spring View)
  ↓ renderMergedOutputModel()
template.make(model).writeTo(response.writer)
```

## Usage in Other Modules

Only two modules directly depend on `grace-views-core`:

| Module | Usage |
|--------|-------|
| `grace-views-json` | JSON views (`.gson` files using `StreamingJsonBuilder`) — declares `grace-views-core` as `api` and provides a concrete `ResolvableGroovyTemplateEngine` subclass |
| `grace-views-markup` | Markup views (`.gml` files using `MarkupTemplateEngine`) — same pattern |

Both modules extend the abstract classes and interfaces from `grace-views-core` to provide their specific template syntax, compiler, and Spring Boot auto-configuration.
