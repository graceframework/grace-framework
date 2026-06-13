# Grace Web MVC

## Module Overview

The `grace-web-mvc` module is a core web MVC integration layer in the Grace Framework, providing controller response handling, exception management, and domain binding APIs that were relocated from `grace-plugin-controllers` during the 2024.x refactoring to improve modularity.

## Important APIs

### Response Handling

- **`RenderDynamicMethod`** - Implements the `render` method in controllers for rendering views, templates, text, and other response formats
- **`ValidResponseHandler`** - Handles successful controller action responses
- **`InvalidResponseHandler`** - Handles error/invalid responses from controller actions

### Exception Handling

- **`ControllerExceptionHandlerMetaData`** - Interface for metadata about exception handlers in controllers
- **`DefaultControllerExceptionHandlerMetaData`** - Default implementation of exception handler metadata

### Domain Binding

- **`ControllersDomainBindingApi`** - Provides static methods for binding data to domain classes from web requests, including initialization and autowiring capabilities

## How It Works

The module provides the foundational MVC infrastructure by:

1. **Response Rendering** - `RenderDynamicMethod` processes controller `render()` calls to generate appropriate responses (views, templates, JSON, XML, etc.)
2. **Exception Resolution** - Exception handler metadata enables the framework to map exceptions to appropriate error views or handlers
3. **Data Binding** - `ControllersDomainBindingApi` enables automatic binding of request parameters to domain class instances with support for Spring autowiring
4. **Response Transformation** - Handlers process controller action return values and transform them into HTTP responses

The module was deliberately decoupled from `grace-web-sitemesh` to reduce dependencies and improve modularity.

## Usage in Other Modules

The `grace-web-mvc` module is used by several other Grace modules:

| Module | Dependency Type | Purpose |
|--------|----------------|---------|
| `grace-plugin-controllers` | api | Core controller functionality and MVC integration |
| `grace-plugin-gsp` | api | GSP rendering and view resolution |
| `grace-web-url-mappings` | api | URL mapping and request routing integration |

## Notes

The module underwent significant refactoring in the 2024.x release, where controller APIs were relocated from `grace-plugin-controllers` to `grace-web-mvc` to create a cleaner separation between plugin-specific functionality and core MVC infrastructure. This refactoring also included decoupling from `grace-web-sitemesh` to reduce module dependencies.
