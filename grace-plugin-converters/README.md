# Grace Plugin Converters

The `grace-plugin-converters` module is a Grace Framework plugin that provides JSON and XML conversion capabilities for web applications, enabling easy serialization and deserialization of Groovy objects to/from JSON and XML formats.


## Module Overview

The `grace-plugin-converters` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-api`, `grace-bootstrap`, `grace-core`, `grace-plugin-api`, `grace-util`, and `grace-web`.

It has a runtime dependency on `grace-plugin-controllers` and optional compile-only dependencies on GORM-related modules for domain class support.

## Important APIs

### ConvertersGrailsPlugin

The main plugin class is `ConvertersGrailsPlugin`, which registers the converters functionality with the Grace framework.

### ConvertersExtension

The module provides Groovy extension methods through the `ConvertersExtension` class, which adds conversion capabilities to standard Groovy objects.
