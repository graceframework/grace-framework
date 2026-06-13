# Grace Plugin Interceptors

The `grace-plugin-interceptors` module is a Grace Framework plugin that provides interceptor functionality for web applications, allowing developers to define cross-cutting concerns that execute before, after, or around controller actions through interceptor classes.


## Module Overview

The `grace-plugin-interceptors` module is defined as a plugin in the Grace Framework settings.

## Important APIs

### InterceptorsGrailsPlugin

The main plugin class `InterceptorsGrailsPlugin` extends `Plugin` and implements `PriorityOrdered` with order 80. 

**Key Features:**

- **Dependencies**: Depends on `controllers` and `urlMappings` plugins
- **Load Order**: Loads after the `controllers` plugin
- **Watched Resources**: Monitors interceptor files in `app/controllers/**/*Interceptor.groovy` 
- **doWithSpring()**: Registers interceptor beans for all interceptor artefacts with autowiring by name
- **doWithApplicationContext()**: Retrieves the `GrailsInterceptorHandlerInterceptorAdapter` from the mapped interceptor bean
- **onChange()**: Handles hot reloading of interceptor classes by redefining beans and updating the interceptor adapter

### InterceptorsPluginConfiguration

Spring Boot auto-configuration class that provides interceptor integration beans.

## Notes

The plugin uses Spring's `MappedInterceptor` to integrate with the Spring MVC handler interceptor chain, allowing interceptors to be applied to specific URL patterns. The `GrailsInterceptorHandlerInterceptorAdapter` serves as the bridge between Grace interceptors and Spring's interceptor mechanism.
