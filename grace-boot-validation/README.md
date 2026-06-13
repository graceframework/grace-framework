# Grace Boot Validation

`grace-boot-validation` is a Spring Boot auto-configuration module that provides validation functionality for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's validation functionality more focused and better integrated with Spring Boot's module structure.

The module depends on `grace-plugin-validation` (which contains the core validation functionality), Spring Boot starter, Tomcat EL implementation, and Hibernate Validator.

The validation capabilities are primarily provided through the `grace-plugin-validation` module, which `grace-boot-validation` wraps. The `grace-plugin-validation` module itself depends on:

- `grace-core` - Core Grace framework functionality
- `grace-plugin-api` - Plugin API (compile only)
- `grace-util` - Utility classes
- `grace.datastore.gorm.validation` - GORM validation support
