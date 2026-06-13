# Grace Views JSON

The `grace-views-json` module provides JSON view rendering for Grace applications using `.gson` files (Groovy-based JSON templates backed by Groovy's `StreamingJsonBuilder`).


## Module Purpose

JSON views let you write `.gson` files in `grails-app/views/` that produce JSON responses. The file extension is `gson` and the base template class is `JsonViewWritableScript`.

Configuration is bound to `grails.views.json.*` and defaults to `compileStatic = true`, with MIME types `application/json` and `application/hal+json`.


## Important APIs

### 1. `JsonView` trait (`org.grails.views.json.api.JsonView`)

The core trait injected into every `.gson` script. It extends `GrailsView` and provides:

| Property/Method | Type | Purpose |
|-----------------|------|---------|
| `json` | `StreamingJsonBuilder` | The builder used to write JSON output |
| `g` | `GrailsJsonViewHelper` | Helper for rendering objects/templates |
| `hal` | `HalViewHelper` | HAL+JSON rendering helper |
| `jsonapi` | `JsonApiViewHelper` | JSON API spec rendering helper |
| `tmpl` | `TemplateRenderer` | Template namespace for rendering sub-templates |
| `inherits(Map)` | method | Specify a parent template to inherit from |
| `json(Closure)` | method | Write a root JSON object |
| `json(List)` | method | Write a root JSON array |
| `json(Map)` | method | Write a root JSON object from a map |

### 2. `GrailsJsonViewHelper` interface (`org.grails.views.json.api.GrailsJsonViewHelper`)

Accessed as `g` inside a view. Key methods:

- `render(Object object)` — renders a domain object to JSON, skipping lazy/internal properties
- `render(Object object, Map arguments)` — with `includes`/`excludes` lists
- `render(Object object, Map arguments, Closure customizer)` — with a `StreamingJsonDelegate` customizer
- `render(Map arguments)` — renders a named template/collection
- `inline(Object object, ...)` — renders an object inline within the current JSON object (no wrapping braces)

### 3. `HalViewHelper` interface (`org.grails.views.json.api.HalViewHelper`)

Accessed as `hal` inside a view. Provides the same `render`/`inline` methods as `GrailsJsonViewHelper` but also outputs HAL `_links`. Additional methods:

- `links(Object object)` / `links(Map model)` — generate HAL `_links` block
- `paginate(Object, Integer total, ...)` — generate HAL pagination links
- `embedded(Object object)` / `embedded(Closure)` — generate HAL `_embedded` block
- `type(String name)` — set the HAL response content type

### 4. `JsonApiViewHelper` interface (`org.grails.views.json.api.JsonApiViewHelper`)

Accessed as `jsonapi` inside a view. Provides `render(Object)` and `render(Object, Map)` for JSON API spec output.

### 5. `JsonViewTemplateEngine` (`org.grails.views.json.JsonViewTemplateEngine`)

Extends `ResolvableGroovyTemplateEngine`. Responsible for:

- Compiling `.gson` scripts with `JsonViewsTransform` AST and optional `@CompileStatic`
- Building a `JsonGenerator` with built-in converters for Java time types (`Instant`, `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `OffsetDateTime`, `LocalTime`, `OffsetTime`, `Period`)
- Creating `JsonViewTemplate` instances

### 6. `JsonViewResolver` (`org.grails.views.json.mvc.JsonViewResolver`)

Extends `SmartViewResolver`. On `@PostConstruct`, registers two renderers into the `RendererRegistry`:

- `ErrorsJsonViewRenderer` — renders Spring `Errors` objects as JSON
- `JsonViewJsonRenderer` — the default JSON renderer for all objects (falls back to default if no view found)

### 7. `JsonViewTest` trait (`org.grails.views.json.test.JsonViewTest`)

A Spock/JUnit test trait for unit-testing `.gson` views. Provides:

- `render(String source)` / `render(String source, Map model)` — render inline template source
- `render(Map arguments)` — render a view/template from `grails-app/views/` by name
- Returns `JsonRenderResult` with `.json` (parsed), `.jsonText`, `.headers`, `.contentType`, `.status`

### 8. Auto-configuration (`JsonViewAutoConfiguration`)

Registers three beans:

- `jsonApiIdRenderStrategy` — `DefaultJsonApiIdRenderer`
- `jsonTemplateEngine` — `JsonViewTemplateEngine`
- `jsonSmartViewResolver` — `JsonViewResolver`
- `jsonViewResolver` — `GenericGroovyTemplateViewResolver` (Spring MVC view resolver)


## How It Works

```
grails-app/views/book/show.gson
  ↓ (build: JsonViewCompilerTask via org.graceframework.grace-json Gradle plugin)
  ↓ (runtime: JsonViewTemplateEngine.createTemplate())
Compiled class extends JsonViewWritableScript (implements JsonView)
  ↓ wrapped in JsonViewTemplate (holds JsonGenerator + JsonApiIdRenderStrategy)
  ↓ JsonViewResolver.resolveView(uri, locale)
  ↓ GenericGroovyTemplateViewResolver → Spring MVC view resolution
  ↓ template.make(model).writeTo(response.writer)
    → StreamingJsonBuilder writes JSON directly to response
```

When `respond book` is called in a controller, `JsonViewJsonRenderer` checks for a matching `.gson` view. If found, it renders it; otherwise it falls back to the default JSON renderer.

The Gradle plugin `org.graceframework.grace-json` compiles `.gson` files at build time via `JsonViewCompilerTask`, using `JsonViewCompiler` as the compiler class and `org.grails.views.json.JsonViewTemplate` as the script base name.


## 2024.0 Refactoring

- Package renamed from `grails.plugin.json.view` → `org.grails.views.json`
- Removed custom `StreamingJsonBuilder`/`JsonOutput`/`JsonGenerator` in favor of native `groovy.json.*` API; added `JsonWritable` and `JsonToken`
