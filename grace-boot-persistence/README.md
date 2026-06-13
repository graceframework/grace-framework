# Grace Boot Persistence

`grace-boot-persistence` is a Spring Boot auto-configuration module that provides core persistence functionality for Grace applications by aggregating domain class support, datasource configuration, and GORM (Grace Object Relational Mapping) capabilities.

The module depends on `grace-plugin-domain-class` (for domain class artefact handling), `grace-plugin-datasource` (for datasource configuration), and Grace Datastore GORM libraries for the core GORM functionality.

The `grace-boot-persistence` module serves as a foundational persistence layer that:

- Provides domain class artefact support through `grace-plugin-domain-class`, which includes GORM AST transformations and entity trait injection
- Offers datasource configuration through `grace-plugin-datasource` for database connection management
- Integrates with Grace Datastore GORM for the core object-relational mapping capabilities

This module acts as a base persistence starter that can be extended by more specific persistence implementations like `grace-boot-hibernate` (for SQL databases) and `grace-boot-mongodb` (for document databases).
