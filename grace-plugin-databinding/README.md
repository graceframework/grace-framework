# Grace Plugin Databinding

The `grace-plugin-databinding` module is a Grace Framework plugin that provides data binding capabilities, enabling automatic population of object properties from request parameters, JSON, XML, and other data sources with support for type conversion and validation integration.


## Module Overview

The `grace-plugin-databinding` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-api`, `grace-web`, and `grace-web-databinding`.

## Important APIs

### DataBindingGrailsPlugin

The main plugin class `DataBindingGrailsPlugin` extends `AbstractDataBindingGrailsPlugin` and implements `PriorityOrdered` with order 65.

### AbstractDataBindingGrailsPlugin

The abstract base class defines default date formats for JSR-310 (Java 8 Date/Time API) data binding, including formats for offset zoned date time, local date time, local date, and local time.

**Default Date Formats:**

- `yyyy-MM-dd HH:mm:ss.S`
- `yyyy-MM-dd'T'HH:mm:ss'Z'`
- `yyyy-MM-dd HH:mm:ss.S z`
- `yyyy-MM-dd'T'HH:mm:ss.SSSX`
- JSR-310 formats (ISO_LOCAL_DATE_TIME, ISO_OFFSET_DATE_TIME, etc.)

### DataBindingConfiguration

According to the wiki documentation, this Spring Boot auto-configuration class registers essential beans for data binding functionality, including:

- `GrailsWebDataBinder` - Web-specific data binder with converters and listeners
- `DataBindingSourceRegistry` - Registry for data source creators
- `XmlDataBindingSourceCreator` - For XML data binding
- `JsonDataBindingSourceCreator` - For JSON data binding
- `HalJsonDataBindingSourceCreator` - For HAL JSON data binding

## Notes

**Refactoring History:**

In the 2024.x release, the `grace-web-databinding` module was restored and decoupled from `grace-web` to improve modularity. The `GrailsParameterMap` and `PropertyEditorRegistryUtils` were updated to use `DataBinder.DEFAULT_DATE_FORMAT` instead of direct dependencies.

**Relationship with grace-databinding:**

The `grace-databinding` module contains the core data binding code with `SimpleDataBinder` as the main class, while the web-specific `GrailsWebDataBinder` (which extends `SimpleDataBinder`) is defined in the `grace-web-databinding` module. The `grace-plugin-databinding` plugin provides the Spring Boot auto-configuration that ties these components together.
