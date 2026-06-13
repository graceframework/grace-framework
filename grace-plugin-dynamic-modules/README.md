# grace-plugin-dynamic-modules

The `grace-plugin-dynamic-modules` module is a Grace Framework plugin that provides a dynamic modules system, allowing plugins to define granular sub-components (module descriptors) for creating modular and maintainable applications.


## Module Overview

The `grace-plugin-dynamic-modules` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-api`, `grace-bootstrap`, and `grace-plugin-api`.


## Important APIs

### DynamicModulesGrailsPlugin

The main plugin class `DynamicModulesGrailsPlugin` extends `Plugin` and provides the dynamic modules functionality.

### DynamicGrailsPlugin Interface

The core interface for dynamic plugins, defined in `grace-plugin-api`, which plugins implement to support dynamic modules.

**Key Methods:**

- `doWithDynamicModules()` - Closure hook for defining modules
- `getProvidedModules()` - Returns provided modules
- `addModuleDescriptor(String type, Map<String, Object> args)` - Adds a module descriptor
- `getModuleDescriptors()` - Returns all module descriptors
- `getModuleDescriptor(String key)` - Returns a specific module descriptor by key

### DynamicBinaryGrailsPlugin

Implementation of `DynamicGrailsPlugin` for binary (pre-compiled) plugins that handles module descriptor registration via DSL-like syntax.

### ModuleDescriptor System

The module uses a descriptor pattern where plugins can register different types of modules:

**WebSectionModuleDescriptor** - Represents a section in a web interface with properties like `location` and `weight`.

**WebInterfaceManager** - Manager interface for querying enabled module descriptors from the plugin manager.

### DynamicModulesAutoConfiguration

Spring Boot auto-configuration class that provides the `WebInterfaceManager` bean.

### GrailsPluginManager Integration

The plugin manager provides methods for querying module descriptors across all loaded plugins:

- `getModuleDescriptors()` - Gets all module descriptors from dynamic plugins
- `getEnabledModuleDescriptorsByClass(Class<D> descriptorClazz)` - Gets enabled descriptors of a specific type
