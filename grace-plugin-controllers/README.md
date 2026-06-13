# Grace Plugin Controllers

The `grace-plugin-controllers` module is a Grace Framework plugin that provides the core MVC (Model-View-Controller) functionality, including the `Controller` trait, request handling, response rendering, and Spring Boot auto-configuration for web applications.


## Module Overview

The `grace-plugin-controllers` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-api`, `grace-bootstrap`, `grace-core`, `grace-plugin-databinding`, `grace-plugin-mimetypes`, `grace-plugin-validation`, `grace-web-gsp`, `grace-web-mvc`, and `grace-web-url-mappings`.

## Important APIs

### Controller Trait

The `Controller` trait is the primary API that classes implement to become web controllers in Grace applications.

**Implemented Interfaces:**

- `ResponseRenderer` - Handles response rendering
- `ResponseRedirector` - Handles response redirects
- `RequestForwarder` - Handles request forwarding
- `DataBinder` - Provides data binding capabilities
- `WebAttributes` - Web-specific attributes
- `ServletAttributes` - Servlet-specific attributes

### ControllersGrailsPlugin

The main plugin class `ControllersGrailsPlugin` extends `Plugin` and implements `PriorityOrdered` with order 50.

**Key Features:**

- **Watched Resources**: Monitors controller files in `app/controllers/**/*Controller.groovy`, and plugin directories
- **Dependencies**: Depends on `core` and `i18n` plugins
- **Bean Configuration**: Configures controller beans with lazy initialization, scope, and autowiring in `doWithSpring()`
- **Hot Reloading**: Handles controller changes via `onChange()` method

### ControllersPluginConfiguration

Spring Boot auto-configuration class that provides essential web infrastructure beans.

**Provided Beans:**

- `GrailsDispatcherServlet` - Custom dispatcher servlet for Grace
- `GrailsWebRequestFilter` - Filter that binds GrailsWebRequest to thread context
- `GrailsExceptionResolver` - Exception handling with URL mappings support
- `CompositeViewResolver` - Composite view resolver for multiple view technologies
- `WebMvcConfigurer` - Configures static resource handlers for `/static/**` and `/webjars/**`
- `OrderedHiddenHttpMethodFilter` - Support for hidden HTTP method parameter
- `StackTraceFilterer` - Filters stack traces in error pages

## Notes

**Refactoring History:**

In the 2024.x release, several controller-related APIs were relocated from `grace-plugin-controllers` to `grace-web-mvc` to improve modularity, including `InvalidResponseHandler`, `ValidResponseHandler`, `ControllerExceptionHandlerMetaData`, `DefaultControllerExceptionHandlerMetaData`, `RenderDynamicMethod`, and `ControllersDomainBindingApi`.

Additionally, `BootstrapClassRunner` was moved from `grace-plugin-controllers` to `grace-boot` and renamed to `GrailsBootstrapClassRunner`.
