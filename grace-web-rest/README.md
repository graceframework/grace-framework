# Grace Web REST

## Module Overview

The `grace-web-rest` module is a web-specific REST integration layer in the Grace Framework, created during the 2024.x modularization refactoring to provide core REST rendering and link management APIs that were relocated from `grace-plugin-rest`.

## Important APIs

### Link Management

- `Link` - Represents hypermedia links in REST responses, relocated from `grace-plugin-rest`
- `Linkable` - Interface for entities that can generate hypermedia links

### Renderer Customization

- `RendererRegistryCustomizer` - Allows customization of the `RendererRegistry` to add user-defined renderers
- `DefaultRendererRegistryCustomizer` - Default implementation of the renderer registry customizer

### Rendering Infrastructure

- Classes in the `org.grails.web.rest.render` package (renamed from `org.grails.plugins.web.rest.render`) provide the core rendering infrastructure for REST responses

## How It Works

The module provides the foundational REST infrastructure by:

1. Renderer Registry Management - `RendererRegistryCustomizer` enables applications to customize how different object types are rendered to JSON, XML, or other formats
2. Hypermedia Support - `Link` and `Linkable` provide HATEOAS (Hypermedia as the Engine of Application State) capabilities for REST APIs
3. Content Negotiation - The rendering infrastructure supports multiple response formats based on content negotiation
4. Extensibility - The customizer pattern allows developers to register custom renderers for specific domain classes or response types

## Usage in Other Modules

The `grace-web-rest` module is used by several other Grace modules:

| Module | Dependency Type | Purpose |
|--------|-----------------|---------|
| `grace-views-core` | api | Core view rendering infrastructure for REST responses |
| `grace-plugin-rest` | api | REST plugin functionality and controller integration |

## Notes

The module was created as part of the 2024.x modularization effort to separate core REST infrastructure from plugin-specific functionality, following the pattern of other `grace-web-*` modules. This refactoring included relocating link management APIs and renaming the rendering package to align with the new modular structure.
