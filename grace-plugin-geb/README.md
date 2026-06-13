# Grace Geb Plugin

The `grace-plugin-geb` module is a Grace Framework plugin that provides Geb functional testing capabilities for Grace applications, including Geb dependencies and a command for generating Geb tests.


## Module Overview

The `grace-plugin-geb` module is defined as a plugin in the Grace Framework settings.

This plugin was merged into the framework in the 2024.x release as part of the core plugins consolidation effort.

## Important APIs

### GebGrailsPlugin

The main plugin class `GebGrailsPlugin` extends `Plugin` and provides Geb functional testing code generation features.

### Install Command

The plugin provides an `install` command (`grace geb:install`) that configures Geb for the application by:

1. Copying `GebConfig.groovy` template to `src/integration-test/resources`
2. Modifying `build.gradle` to add system properties for Geb environment and build reports directory

