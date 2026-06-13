# grace-plugin-i18n

The `grace-plugin-i18n` module is a Grace Framework plugin that provides internationalization (i18n) capabilities, including message source extensions and resource loading utilities for localizing applications.


## Module Overview

The `grace-plugin-i18n` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-api`, `grace-bootstrap`, `grace-plugin-api`, `grace-util`, and `grace-web`.

It has API dependencies on Groovy Ant and Spring Boot autoconfigure.

## Important APIs

### GrailsMessageSourceExtension

The module provides a Groovy extension module named `grails-i18n-module` that adds internationalization capabilities through the `GrailsMessageSourceExtension` class.

### I18nGrailsPlugin

The main plugin class is `I18nGrailsPlugin`, which is referenced in the CLI compiler auto-configuration to automatically add the i18n dependency when the class is missing.

### Resource Loading Utilities

In the 2024.x release, `ClassRelativeClassLoader` and `ClassRelativeResourcePatternResolver` were moved from `grace-spring-boot` to `grace-plugin-i18n` to improve modularity.
