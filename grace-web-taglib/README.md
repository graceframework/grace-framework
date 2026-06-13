# Grace Web Taglib

`grace-web-taglib` is the web-layer taglib module that bridges the low-level `grace-taglib` infrastructure with the servlet/web layer. It provides the traits, bindings, and output context implementations that make taglibs work in a web request context.


## Important APIs

### 1. `TagLibrary` trait (`grails.artefact.TagLibrary`)

The primary trait injected into every taglib class. It extends `WebAttributes`, `ServletAttributes`, and `TagLibraryInvoker`.

Key members:

- `initializeTagLibrary()` — `@PostConstruct` method that enhances the taglib's metaclass with all tag methods (in non-development mode)
- `getOut()` / `setOut(Writer)` — access the current output writer via `OutputEncodingStack`
- `getPageScope()` — returns the `TemplateVariableBinding` for the current request's page scope
- `raw(Object)` — encodes a value using the Raw encoder (bypasses XSS encoding)
- `throwTagError(String)` — throws a `GrailsTagException`
- `propertyMissing(String)` — looks up namespace dispatchers or tag closures from `TagLibraryLookup`

### 2. `TagLibraryInvoker` trait (`grails.artefact.gsp.TagLibraryInvoker`)

A lower-level trait that adds tag invocation capability to any class (e.g., controllers). It extends `WebAttributes`.

Key members:

- `getTagLibraryLookup()` — lazily resolves the `gspTagLibraryLookup` bean from the application context
- `methodMissing(String, Object)` — intercepts unknown method calls and routes them to the matching taglib via `TagLibraryLookup`
- `propertyMissing(String)` — resolves namespace dispatchers (e.g., `g.link(...)`)
- `withCodec(Object, Closure)` — executes a closure with a specific codec applied

### 3. `TagLibraryTraitInjector` (`grails.compiler.traits.TagLibraryTraitInjector`)

An AST `TraitInjector` that injects the `TagLibrary` trait into all `TagLib` artefact classes at compile time.

### 4. `WebRequestTemplateVariableBinding`

The top-level Groovy `Binding` used during GSP evaluation. It lazily resolves web-request-scoped variables from `GrailsWebRequest`: `request`, `response`, `session`, `flash`, `params`, `webRequest`, `application`, `applicationContext`, `grailsApplication`, `actionName`, `controllerName`.

### 5. `WebOutputContext` and related classes (`org.grails.web.taglib.encoder`)

Web-specific implementation of the `OutputContext` interface from `grace-taglib`. Stores the `OutputEncodingStack` and `TemplateVariableBinding` as request attributes on `GrailsWebRequest`.

| Class | Purpose |
|-------|---------|
| `WebOutputContext` | Main web `OutputContext` — reads/writes the encoding stack and binding from the current request |
| `WebOutputContextLookup` | Looks up the `WebOutputContext` for the current thread |
| `WebOutputContextInitializer` | Initializes the output context at the start of a request |
| `WebRequestOutputContext` | Request-scoped variant |

### 6. `StandaloneTagLibraryLookup`

A subclass of `TagLibraryLookup` that listens for `ContextRefreshedEvent` to trigger tag library registration. Used when GSP is run outside a full web application context.

### 7. `LayoutWriterStack`

A utility for "layout" tags — tags that assemble multiple named body parts (e.g., `left`, `center`, `right`) into a larger structure. `writeParts(body)` executes the body closure and returns a `Map` of named part outputs.


## How It Works

```
TagLib class (user code)
  ↓ (AST: TagLibraryTraitInjector)
TagLibrary trait injected at compile time
  ↓ (@PostConstruct: initializeTagLibrary)
TagLibraryMetaUtils.enhanceTagLibMetaClass()
  → registers all tag names as metaclass methods
  → registers namespace properties (e.g., g.link(...))

At runtime (GSP rendering):
  WebOutputContext ← WebOutputContextLookup
  WebRequestTemplateVariableBinding ← GrailsWebRequest
  TagLibraryInvoker.methodMissing → TagLibraryLookup → taglib bean → TagOutput.captureTagOutput()
```
