# Grace Plugin Domain Class

The `grace-plugin-domain-class` module is a Grace Framework plugin that provides domain class artefact handling, GORM (Grace Object Relational Mapping) integration through AST transformations, and compile-time enhancement of domain classes for persistence capabilities.


## Module Overview

The `grace-plugin-domain-class` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-core`, `grace-plugin-api`, `grace-plugin-validation`, and `grace-spring`.

It has API dependencies on Jakarta Persistence API, Grace Datastore core, GORM, and GORM validation.

## Important APIs

### DomainClassArtefactHandler

The artefact handler responsible for identifying domain classes during the build and at runtime.

**Identification Logic:**

A class is considered a domain class if:

- It is annotated with `@grails.artefact.Artefact("Domain")`
- It is located in `app/domain` and is not a JPA entity, Enum, or Inner Class
- It is annotated with GORM-specific entity annotations like `grails.persistence.Entity`

### GrailsDomainClass Interface

Interface representing a persistable Grails domain class with methods for accessing constraints, validators, and ORM mapping information.

**Key Methods:**

- `getConstrainedProperties()` - Returns a map of constraints applied to the domain class
- `getValidator()` / `setValidator()` - Retrieves and sets the validator for the domain class
- `isOwningClass()` - Determines if the domain class is on the owning side of a relationship

### DefaultGrailsDomainClass

Default implementation of `GrailsDomainClass` that bridges the legacy Grails API with the modern GORM `MappingContext`. 

**Key Features:**

- Retrieves `PersistentEntity` from the GORM `MappingContext`
- Handles constraints discovery and validator registration
- Provides a warning that the API should no longer be used to retrieve data about domain classes, recommending the mapping context API instead

### Compile-Time Transformations

**GormTransformer**: A `GrailsArtefactClassInjector` registered via `META-INF/grails.factories` that delegates to `GormEntityTransformation` to inject persistence logic into domain classes.

**EntityTraitInjector**: Injects the `grails.gorm.Entity` trait into any class identified as a `Domain` artefact, allowing domain classes to inherit persistence methods in a type-safe manner.
