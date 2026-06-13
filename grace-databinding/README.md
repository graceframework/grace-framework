# Grace Databinding

`grace-databinding` is a foundational module that contains the core data binding infrastructure for Grace applications, providing the base implementation for binding data from maps to object properties.


### Core Data Binder

`SimpleDataBinder` - The main class in the module that contains much of the core data binding logic. It serves as the base implementation for binding data from maps to object properties.

## Module Relationships

### grace-web-databinding

The `grace-web-databinding` module extends `grace-databinding` with web-specific functionality. It depends on `grace-databinding`, `grace-util`, and `grace-web`. This module contains `GrailsWebDataBinder` which extends `SimpleDataBinder` and defines logic specific to data binding in the context of a Grace application, including GORM special handling.

### grace-plugin-databinding

The `grace-plugin-databinding` module provides Spring Boot auto-configuration for data binding. It depends on `grace-api`, `grace-web`, and `grace-web-databinding`. The plugin is configured via `DataBindingGrailsPlugin` which extends `AbstractDataBindingGrailsPlugin`.
