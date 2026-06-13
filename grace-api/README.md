# Grace API

`grace-api` module serves as the core API layer for the Grace Framework, containing fundamental interfaces that other modules depend on.

It now focuses on core interfaces that define the framework's fundamental abstractions, while implementations and concrete classes have been moved to appropriate modules like `grace-core`.

### GrailsApplication

`GrailsApplication` - The primary interface representing a running Grace application, providing access to application metadata, configuration, and artefacts. Key methods include:

- `getConfig()` - Returns the Config instance
- `getMainContext()` - Returns the Spring context for the application
- `getMappingContext()` - Returns the GORM MappingContext
- `isArtefact(Class)` - Checks if a class is a Grace artefact
- `getAllArtefacts()` - Retrieves all artefact classes
- `getArtefacts(String artefactType)` - Gets all GrailsClass instances for a given artefact type
- `registerArtefactHandler(ArtefactHandler)` - Registers a new artefact handler
- `getArtefactHandlers()` - Obtains all registered artefact handlers

### ArtefactHandler

`ArtefactHandler` - Interface for analyzing conventions within a Grace application, responsible for identifying, creating, and initializing artefacts. Key methods include:

- `getType()` - Returns the artefact type (e.g., "Domain", "Controller")
- `isArtefact(Class)` - Checks if a class is this type of artefact
- `isArtefact(ClassNode)` - Checks if a ClassNode is this type of artefact (for AST processing)
- `newArtefactClass(Class)` - Creates a GrailsClass wrapper for an artefact
- `initialize(ArtefactInfo)` - Initializes when artefact list changes
- `getArtefactForFeature(Object)` - Retrieves artefact by feature (e.g., URI, tag name)

### ArtefactInfo

`ArtefactInfo` - Holder for class-related info and structures relating to an artefact type. Provides access to:

- `getClasses()` - Array of original artefact classes
- `getGrailsClasses()` - Array of GrailsClass wrappers
- `getClassesByName()` - Map of classes by name
- `getGrailsClassesByName()` - Map of GrailsClasses by name
- `getGrailsClass(String name)` - Retrieves a named GrailsClass

### ArtefactTypes

`ArtefactTypes` - Defines constants for standard artefact types in Grace:

- `APPLICATION` - Application artefact
- `BOOTSTRAP` - Bootstrap artefact
- `CONTROLLER` - Controller artefact
- `DOMAIN_CLASS` - Domain class artefact
- `SERVICE` - Service artefact
- `TAG_LIBRARY` - Tag library artefact
- `URL_MAPPINGS` - URL mappings artefact

### GrailsClass

`GrailsClass` - Interface referenced throughout the artefact system as the wrapper class for artefact classes, providing metadata and additional information about artefacts. It is used by `ArtefactHandler.newArtefactClass()` to create wrappers and stored in `ArtefactInfo` structures.

### Config

`Config` - The configuration interface returned by `GrailsApplication.getConfig()`, providing access to application configuration properties.

### Utility Interfaces

`Named` and `Described` - Utility interfaces for objects that have a name and description, relocated from `grace-bootstrap` to `grace-api` in the 2024.x refactoring.
