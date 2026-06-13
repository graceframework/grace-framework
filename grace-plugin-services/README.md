# Grace Plugin Services

The `grace-plugin-services` module is a small but essential Grace plugin that registers Grace service classes as Spring beans and handles service artefact detection.


## Module Purpose

The plugin is registered as `services` via `ServicesGrailsPlugin`. It scans all classes recognized as Grace service artefacts and registers them as Spring beans in the application context, with support for autowiring, scoping, and lazy initialization.

## Important APIs

### 1. `GrailsServiceClass` interface (`grails.core.GrailsServiceClass`)

The core interface representing a Grace service artefact. Extends `InjectableGrailsClass`.

Key members:

- `isTransactional()` — whether the service should be configured with transaction demarcation
- `getDatasource()` — the datasource name this service works with (defaults to `"DEFAULT"`)
- `usesDatasource(String name)` — checks if the service uses a named datasource
- Constants: `DATA_SOURCE`, `DEFAULT_DATA_SOURCE`, `ALL_DATA_SOURCES`

### 2. `DefaultGrailsServiceClass` (`org.grails.core.DefaultGrailsServiceClass`)

The default implementation of `GrailsServiceClass`. Reads the `static transactional` property from the class (defaults to `true`) and the `static datasource` property.

### 3. `ServiceArtefactHandler` (`org.grails.core.artefact.ServiceArtefactHandler`)

Registered via `grails.factories` as the `ArtefactHandler` for services. It detects classes ending in `Service` in the `services/` directory. Importantly, it **excludes** classes annotated with Spring's `@Service` annotation — those are treated as plain Spring beans, not Grace service artefacts.

Key constants:

- `TYPE = ArtefactTypes.SERVICE` (`"Service"`)
- `PATH = "services"` — the directory scanned for service classes
- `PLUGIN_NAME = "services"`

### 4. `ServiceBeanAliasPostProcessor` (`org.grails.plugins.services.ServiceBeanAliasPostProcessor`)

A `BeanFactoryPostProcessor` that registers short-name aliases for plugin-provided services. For example, if a plugin named `ReportingPlugin` provides `PrintingService`, the bean is named `reportingPrintingService`. This post-processor registers a `printingService` alias pointing to it — as long as no other bean with that name exists.

---

## How `ServicesGrailsPlugin` Registers Beans

For each detected `GrailsServiceClass`, the plugin:

- Determines the bean name: if the service is from a plugin and its name doesn't start with the plugin name, it prefixes the plugin name (e.g., `reportingPrintingService`)
- Reads `static scope` and `static lazyInit` properties from the service class
- Registers the bean with `autowire = true`
- Loads after the `hibernate` plugin (so GORM is available)
- Watches `app/services/**/*Service.groovy` for hot-reload via `onChange`
