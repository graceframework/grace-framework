# Grace Boot Plugin

`grace-boot-plugin` is a Spring Boot auto-configuration module that provides plugin system support for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's plugin functionality more focused and better integrated with Spring Boot's module structure.


The module depends on `grace-boot` (the core boot module), `grace-plugin-api` (the plugin API), `grace-plugin-core` (core plugin functionality), and `grace-plugin-dynamic-modules` (Dynamic modules support).

The plugin system capabilities are provided through the dependencies that `grace-boot-plugin` aggregates:

- The `grace-plugin-api` module provides the `DefaultGrailsPluginManager` which handles the loading and management of plugins in the Grace system. A plugin is a Groovy class that has a version and optionally closures called `doWithSpring`, `doWithContext`, and `doWithWebDescriptor` for runtime configuration.

- The `grace-plugin-dynamic-modules` dependency provides support for dynamic module types, which are a powerful feature of Grace's plugin ecosystem that allows for flexible and extensible application architecture.
