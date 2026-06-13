# Grace Views Markup

The `grace-views-markup` module provides XML/markup view rendering for Grace applications using Groovy's [`MarkupTemplateEngine`](https://docs.groovy-lang.org/latest/html/documentation/#_the_markuptemplateengine). View files use the `.gml` extension.


## Important APIs

### `MarkupView` trait (`org.grails.views.markup.api.MarkupView`)

The trait injected into all compiled `.gml` view scripts. Extends `GrailsView` and exposes `getG()` which returns a `GrailsViewHelper` (the `g` variable available inside views for URL generation, etc.).

### `MarkupViewTemplate` (abstract base class)

The script base class for all compiled `.gml` templates. Extends Groovy's `BaseTemplate` and implements both `WritableScript` and `MarkupView`. The file extension is `gml` and the type identifier is `views.gml`.

### `MarkupViewConfiguration` (`@ConfigurationProperties('grails.views.markup')`)

Extends Groovy's `TemplateConfiguration` and implements `GenericViewConfiguration`. Configures:

- Extension: `gml`
- MIME types: `text/xml`, `application/hal+xml`
- Auto-escape: `true`
- Pretty print (auto-indent + auto-newline) in development mode
- Template caching (disabled in development mode)

### `MarkupViewTemplateEngine`

Extends `ResolvableGroovyTemplateEngine`. Wraps Groovy's `MarkupTemplateEngine` internally, delegating template resolution to `PluginAwareTemplateResolver`. Applies `MarkupViewsTransform` (AST) and optionally `MarkupTemplateTypeCheckingExtension` for static compilation. Creates `MarkupViewWritableScriptTemplate` instances. 

### `MarkupViewWritableScriptTemplate`

Extends `GrailsViewTemplate`. Wraps the compiled template class and creates `MarkupView` instances bound to the model on each `make(model)` call.

### `MarkupViewResolver` (`org.grails.views.markup.mvc.MarkupViewResolver`)

Extends `SmartViewResolver`. On `@PostConstruct`, registers `MarkupViewXmlRenderer` for each configured MIME type (`text/xml`, `application/hal+xml`) into the `RendererRegistry`.

### `MarkupViewXmlRenderer`

Extends `DefaultViewRenderer`. Integrates with the Grace renderer framework for XML content negotiation — when a controller calls `respond object` and the client requests XML, this renderer finds and renders the matching `.gml` view.

### `MarkupViewCompiler`

Extends `AbstractGroovyTemplateCompiler`. Used at build time to pre-compile `.gml` files to JVM classes. Applies Groovy's `TemplateASTTransformer` and `MarkupTemplateTypeCheckingExtension` for static type checking.

### `MarkupViewAutoConfiguration`

Spring Boot auto-configuration entry point. Registers three beans: `markupTemplateEngine` (`MarkupViewTemplateEngine`), `smartMarkupViewResolver` (`MarkupViewResolver`), and `markupViewResolver` (`GenericGroovyTemplateViewResolver` for Spring MVC).


## How It Works

```
.gml file (Groovy MarkupTemplateEngine DSL)
  ↓ (build time: MarkupViewCompiler via compileMarkupViews Gradle task)
  ↓ (runtime: MarkupViewTemplateEngine.createTemplate(url))
Compiled class extends MarkupViewTemplate (BaseTemplate + WritableScript + MarkupView)
  ↓
MarkupViewWritableScriptTemplate (wraps class)
  ↓ (Spring MVC: GenericGroovyTemplateViewResolver → MarkupViewResolver)
MarkupViewWritableScriptTemplate.make(model) → MarkupView instance
  ↓
writeTo(response.writer) → XML output
```

Content negotiation: when a client sends `Accept: text/xml` or `Accept: application/hal+xml`, `MarkupViewXmlRenderer` is invoked, which finds the matching `.gml` view via `MarkupViewResolver`.


## Gradle Plugin

The `org.graceframework.grace-markup` Gradle plugin (implemented by `GrailsMarkupViewsPlugin`) adds the `compileMarkupViews` task using `MarkupViewCompilerTask`, which pre-compiles `.gml` files using `MarkupViewCompiler` as the compiler class and `org.grails.views.markup.MarkupViewTemplate` as the script base name.


## Usage in Other Modules

| Module | Usage |
|--------|-------|
| `grace-boot-rest` | Declares it as `api` — the REST boot module bundles markup views as part of the REST stack alongside `grace-views-json` |


## 2024.0 Refactoring

The package was renamed from `grails.plugin.markup.view` → `org.grails.views.markup` when `grace-views` was merged into the main framework repository.
