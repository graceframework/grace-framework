## Grace Plugin Taglibs

The `grace-plugin-taglibs` module provides the tag library infrastructure for Grace applications — both the built-in tag libraries (like `g:link`, `g:form`, `g:render`) and the compile-time/runtime machinery that makes custom tag libraries work.

It was created in Grace 2024.0.0-M2 by extracting all `*TagLib` classes from `grace-plugin-gsp` into a dedicated module.


## How It Works

The module operates at three levels:

### 1. Compile-time: Artefact Detection

`TagLibArtefactHandler` detects TagLib artefacts. A class is recognized as a TagLib if it:
- Is annotated with `@grails.gsp.TagLib` (triggers `TagLibArtefactTypeAstTransformation`)
- Is annotated with `@Artefact("TagLib")`
- Lives in `app/taglibs/` and has a `TagLib` suffix

The class must be concrete (not abstract) and must end in `TagLib`.

### 2. Compile-time: AST Transformation

`TagLibraryTransformer` (registered as a `ClassInjector`) processes every TagLib class. For each Closure property that is a tag, it generates **5 overloaded methods**:

- `tagName()`
- `tagName(Map)`
- `tagName(Closure)`
- `tagName(Map, Closure)`
- `tagName(Map, CharSequence)`

This allows tags to be called with any combination of attributes and body.

### 3. Compile-time: Trait Injection

Two `TraitInjector`s are registered:

- `TagLibraryTraitInjector` — injects the `TagLibrary` trait into all `TagLib` artefacts, giving them `out`, `throwTagError()`, `raw()`, `withCodec()`, and namespace dispatch via `propertyMissing`
- `ControllerTagLibraryTraitInjector` — injects `TagLibrary` into controllers, so controllers can call tags directly (e.g., `link(controller:'foo')`)

### 4. Runtime: Tag Lookup

`TagLibraryLookup` (in `grace-taglib`) maintains a map of `namespace → tagName → taglib bean`. It is populated at startup from all registered `GrailsTagLibClass` artefacts. `NamespacedTagDispatcher` objects (one per namespace) are created to allow `g.link(...)` style calls.

The `TemplateNamespacedTagDispatcher` (registered via `grails.factories`) handles the `g:render` namespace specifically.


## Important APIs

### Built-in Tag Libraries

All live in `org.grails.plugins.web.taglib`:

| TagLib | Namespace | Key Tags |
|--------|-----------|----------|
| `ApplicationTagLib` | `g` | `link`, `createLink`, `resource`, `set`, `if`, `else`, `each`, `collect`, `findAll`, `grep`, `while`, `unless`, `pageProperty`, `meta`, `layoutTitle`, `layoutHead`, `layoutBody` |
| `FormTagLib` | `g` | `form`, `textField`, `passwordField`, `hiddenField`, `select`, `checkBox`, `radioGroup`, `datePicker`, `submitButton`, `actionSubmit` |
| `RenderTagLib` | `g` | `render`, `include`, `layoutBody`, `layoutHead`, `layoutTitle`, `applyLayout`, `paginate`, `sortableColumn` |
| `ValidationTagLib` | `g` | `hasErrors`, `eachError`, `renderErrors`, `message` |
| `FormatTagLib` | `g` | `formatDate`, `formatNumber`, `formatBoolean`, `encodeAs` |
| `JavascriptTagLib` | `g` | `javascript`, `escapeJavascript` |
| `UrlMappingTagLib` | `g` | `link`, `createLink`, `createLinkTo` |
| `CountryTagLib` | `g` | `country`, `countrySelect` |
| `SitemeshTagLib` | `g` | `layoutBody`, `layoutHead`, `layoutTitle` |
| `PluginTagLib` | `g` | `pluginContextPath` |



### `GrailsTagLibClass` interface

Represents a TagLib artefact. Key methods: `hasTag(String)`, `getTagNames()`, `getNamespace()`, `getTagNamesThatReturnObject()`, `getEncodeAsForTag(String)`.

### `TagInvocationContextCustomizer`

A `@FunctionalInterface` that allows customizing tag invocation context (namespace, tag name, attrs) before a tag is invoked. Two implementations are registered: `ApplicationTagInvocationContextCustomizer` and `RenderTagInvocationContextCustomizer`.

### Auto-configuration

`GrailsTagLibraryAutoConfiguration` is the Spring Boot auto-configuration entry point for the module.
