# Grace Bootstrap

`grace-bootstrap` is a foundational module that provides core configuration, settings, and bootstrap infrastructure for Grace applications.

### Configuration APIs

* `Settings` - Framework settings and configuration constants, provides constants for various framework settings like logging and exception handling.

* `ConfigProperties` - Configuration properties handling.

* `NavigableMap` / `NavigableMapConfig` - Navigable configuration maps that provide hierarchical configuration access.

* `CompositeConfig` - Composite configuration implementation that aggregates multiple configuration sources.

* `EnvironmentAwarePropertySource` - Environment-aware property source for Spring's Environment abstraction.

* `PropertySourcesConfig` - Property sources configuration implementation.

### Configuration Loaders

* `GroovyConfigPropertySourceLoader` - Loads configuration from Groovy config files.

* `YamlPropertySourceLoader` - Loads configuration from YAML files.

### Build and CLI APIs

* `BuildSettings` - Build-time settings for the CLI and build system, providing properties like `APP_BASE_DIR`, `APP_DIR`, `PROJECT_TARGET_DIR`, etc. Used extensively by the Gradle plugin and console.

* `CodeGenConfig` - Code generation configuration used by the CLI for scaffolding and code generation.

* `GrailsFactoriesLoader` - Factories loader for SPI-like functionality.

* `GrailsResourceUtils` - Resource utilities for handling file paths and resources.
