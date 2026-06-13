# Grace Taglib

`grace-taglib` is the low-level runtime engine for tag library execution in Grace. It provides the core infrastructure for tag lookup, dispatch, output capture, and variable binding — without any web or servlet dependencies.


## Module Purpose

`grace-taglib` is a foundational module that sits below `grace-gsp` and `grace-web-taglib`. It has minimal dependencies:

- `grace-api`, `grace-core`, `grace-encoder`

It contains no servlet or Spring MVC dependencies — it is purely the tag execution engine.


## How It Works

```
TagLibraryLookup (registry)
    ↓ afterSingletonsInstantiated()
    scans GrailsTagLibClass artefacts → builds namespace:tagName → bean map
    creates DefaultNamespacedTagDispatcher per namespace (with ExpandoMetaClass)
         ↓
g.link(action:'foo')  →  DefaultNamespacedTagDispatcher.methodMissing()
         ↓
TagLibraryMetaUtils.methodMissingForTagLib()
         ↓
TagOutput.captureTagOutput(lookup, namespace, tagName, attrs, body, outputContext)
         ↓
  sets up OutputEncodingStack → invokes tag Closure → returns captured output
```


## Important APIs

### `TagLibraryLookup`

The central registry. Implements `SmartInitializingSingleton` so it initializes after all Spring beans are ready.

Key methods:

- `registerTagLib(GrailsTagLibClass)` — registers a taglib and its tags into the namespace map
- `lookupTagLibrary(namespace, tagName)` — returns the taglib bean for a given namespace+tag
- `lookupNamespaceDispatcher(namespace)` — returns the `NamespacedTagDispatcher` for a namespace
- `hasNamespace(namespace)`, `getAvailableNamespaces()`, `getAvailableTags(namespace)`
- `doesTagReturnObject(namespace, tagName)`, `getEncodeAsForTag(namespace, tagName)`

Custom `NamespacedTagDispatcher` implementations can be registered via `grails.factories` and will override the default dispatcher for a namespace.


### `TagOutput`

Static utility class that executes a tag invocation and captures its output. The central method is:

```java
TagOutput.captureTagOutput(lookup, namespace, tagName, attrs, body, outputContext)
```

It:
1. Looks up the taglib bean via `TagLibraryLookup`
2. Pushes a new `OutputEncodingStack` frame with a `GroovyPageTagWriter`
3. Invokes the tag `Closure` (1-arg or 2-arg form)
4. Returns either the captured buffer or the tag's return value (for `returnObjectForTags`)

Also defines `DEFAULT_NAMESPACE = "g"` and `EMPTY_BODY_CLOSURE`.


### `NamespacedTagDispatcher` / `DefaultNamespacedTagDispatcher`

`NamespacedTagDispatcher` is the interface that enables `g.link(...)` syntax in controllers and taglibs:

`DefaultNamespacedTagDispatcher` creates a per-instance `ExpandoMetaClass` and registers all tag methods on it at initialization time. Its `methodMissing` delegates to `TagLibraryMetaUtils.methodMissingForTagLib`.


### `TagLibraryMetaUtils`

Utility class for dynamically enhancing metaclasses with tag methods. Key methods:

- `enhanceTagLibMetaClass(taglib, lookup)` — adds all tag methods + namespace properties to a taglib's metaclass
- `registerTagMetaMethods(mc, lookup, namespace)` — registers 5 overloaded variants per tag (`Map+Closure`, `Map+CharSequence`, `Map`, `Closure`, no-arg)
- `methodMissingForTagLib(...)` — handles `methodMissing` at runtime by looking up the tag and optionally caching the method on the metaclass


### `TagBodyClosure`

Wraps the body of a tag invocation. When called, it captures the body's output into a `GroovyPageTagWriter`. Supports passing a `Map` of variables to the body (e.g., `body(foo: 1, bar: 'test')`), which temporarily adds them to the GSP binding.


### `TagInvocationContext` / `TagInvocationContextCustomizer`

`TagInvocationContext` holds the namespace, tag name, and attrs for a tag call. `TagInvocationContextCustomizer` is a `@FunctionalInterface` that allows intercepting and modifying the context before the tag executes.


### `TemplateVariableBinding`

A Groovy `Binding` subclass used as the variable scope during GSP template evaluation. Supports a parent binding chain with variable caching — variables found in a parent binding are cached locally for performance.


### `encoder` sub-package

| Class | Purpose |
|-------|---------|
| `OutputContext` | Interface representing the current output context during GSP rendering |
| `OutputContextLookupHelper` | Static helper to get the current `OutputContext` |
| `OutputEncodingStack` | Stack of writers/encoders pushed/popped during tag rendering |
| `WithCodecHelper` | Merges and applies codec/encoding settings for tags |


## Usage in Other Modules

Only two modules directly depend on `grace-taglib`:

| Module | Usage |
|--------|-------|
| `grace-gsp` | GSP rendering engine uses `TagLibraryLookup`, `TagOutput`, `TemplateVariableBinding`, and the encoder stack |
| `grace-web-taglib` | Web-layer taglib support (servlet-aware) builds on top of `grace-taglib` |

`grace-plugin-taglibs` (which depends on `grace-web-taglib`) uses `TagLibraryLookup` and `TagLibraryMetaUtils` to register built-in taglibs (`ApplicationTagLib`, `FormTagLib`, `RenderTagLib`, etc.) and inject the `TagLibrary` trait into taglib and controller classes.
