# Grace Scaffolding Core

This user guide will cover how this library works along with how developers can extend it to completely customize the results.
The `grace-scaffolding-core` module is the low-level markup rendering engine that underpins the scaffolding system. It generates markup based on domain class *definitions* (not actual data instances), which is then processed by a template engine to produce the final views.


## Module Purpose

It renders three types of views:

- **list** — tabular listing of domain instances
- **show** — detail view of a single instance
- **create/edit** — form for creating or editing an instance

The rendering rules follow the same conventions as the `grace-plugin-fields` plugin.


## Important APIs

The key classes are:

| Class | Role |
|-------|------|
| `DomainMarkupRenderer` | Top-level interface for rendering markup for a domain class (list/show/create/edit views) |
| `DomainMarkupRendererImpl` | Default implementation — introspects `PersistentEntity` to determine which properties to render and in what order |
| `ContextMarkupRenderer` | Interface for rendering markup within a specific context (e.g., inside a form or table row) |
| `ContextMarkupRendererImpl` | Default implementation of `ContextMarkupRenderer` |
| `DelegatingBeanPropertyAccessorImpl` | Accesses bean properties via delegation, used to read property metadata for rendering decisions |

These classes work with GORM's `PersistentEntity` and `PersistentProperty` APIs to introspect domain class structure at runtime, without needing actual data instances.


## Used in Other Modules

Only two modules depend on `grace-scaffolding-core`:

| Module | Usage |
|--------|-------|
| `grace-plugin-fields` | The Fields plugin uses the markup renderer infrastructure to render individual field templates |
| `grace-plugin-scaffolding` | The Scaffolding plugin uses it to generate full CRUD views and controller templates |

