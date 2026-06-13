# Grace Plugin REST

The `grace-plugin-rest` module provides the core REST support for Grace applications, including the `@Resource` annotation, `RestfulController` base class, the `RestResponder` trait, and a renderer registry for content negotiation.


## Module Purpose

The plugin is registered as `restResponder` and auto-configured via `RestResponderPluginConfiguration`. It scans domain classes annotated with `@Resource` and registers generated controllers for them.

## Important APIs

### 1. `@Resource` annotation (`grails.rest.Resource`)

Applied to a domain class to automatically expose it as a RESTful resource. The `ResourceTransform` AST transformation generates a controller at compile time.

Key attributes:

- `readOnly` — disables write operations (POST/PUT/PATCH/DELETE)
- `formats` — allowed response formats (default: `['json', 'xml']`)
- `uri` — auto-registers a URL mapping for the resource
- `namespace` — namespaced URL mapping
- `superClass` — the generated controller's superclass (default: `RestfulController`)

### 2. `RestfulController<T>` (`grails.rest.RestfulController`)

Base class providing full CRUD actions for a RESTful API. Extend this to get `index`, `show`, `create`, `save`, `edit`, `update`, `patch`, and `delete` actions out of the box.

Key protected methods you can override:

- `queryForResource(id)` — fetch a single resource
- `listAllResources(params)` / `countResources()` — list/count resources
- `createResource()` / `saveResource(T)` / `updateResource(T)` / `deleteResource(T)` — lifecycle hooks

### 3. `RestResponder` trait (`grails.artefact.controller.RestResponder`)

Injected into all controllers via `RestResponderTraitInjector`. Provides the `respond` method for content-negotiated responses.

This trait injection is registered in `grails.factories`:

### 4. `RendererRegistry` / `Renderer<T>` (`grails.rest.render`)

The `RendererRegistry` bean (auto-configured by `RestResponderPluginConfiguration`) manages all registered renderers. Renderers handle serializing objects to specific formats (JSON, XML, HAL, Atom, etc.).

The `RendererRegistryCustomizer` interface allows other modules to register additional renderers.

Built-in renderer packages under `grails.rest.render`:
- `json/` — JSON renderers
- `xml/` — XML renderers
- `hal/` — HAL (Hypertext Application Language) renderers
- `atom/` — Atom feed renderers
- `errors/` — validation error renderers
- `html/` — HTML renderers (`DefaultHtmlRenderer`)

### 5. `ResourceTransform` AST transformation

Processes `@Resource`-annotated domain classes at compile time to generate a `RestfulController` subclass and register URL mappings.
