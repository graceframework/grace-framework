# Grace Plugin Database Migration

The `grace-plugin-database-migration` module is a Grace Framework plugin that provides database schema migration capabilities using Liquibase, enabling developers to manage database changes in a version-controlled manner with Groovy DSL support.


## Module Overview

This module was merged into the framework in the 2024.x release, previously existing as a separate plugin.

## Important APIs

### DatabaseMigrationGrailsPlugin

The main plugin class `DatabaseMigrationGrailsPlugin` extends `Plugin` and configures Liquibase integration.

**Key Features:**

- doWithSpring(): Calls `configureLiquibase()` to inject ApplicationContext and Config into `GroovyChangeLogParser`
- Utility Methods: Provides `getDataSourceName()` and `isDefaultDataSource()` for handling multiple data sources

### DatabaseMigrationAutoConfiguration

Spring Boot auto-configuration class that provides Liquibase integration beans.

**Configuration:**

- Runs before `LiquibaseAutoConfiguration`
- Conditional on `SpringLiquibase.class` and `DatabaseChange.class`
- Can be enabled/disabled via `grails.plugin.databasemigration.enabled` property

**Provided Beans:**

- `SpringLiquibase` (as `GrailsLiquibase`) - Configured with properties from `DatabaseMigrationProperties`
- `LiquibaseConnectionDetails` - For configuring Liquibase data source connection

### DatabaseMigrationProperties

Configuration properties class with prefix `grails.plugin.databasemigration`.

**Key Properties:**

- `enabled` (default: true) - Enable/disable the plugin
- `updateOnStart` (default: false) - Run migrations on application startup
- `dropOnStart` (default: false) - Drop database on startup
- `updateOnStartFileName` (default: "db/migrations/changelog.groovy") - Changelog file location
- `updateOnStartContexts` - Liquibase contexts for startup migration
- `databaseChangeLogTableName` - Custom changelog table name
- `databaseChangeLogLockTableName` - Custom lock table name

### DatabaseMigrationCommand

Trait that provides common functionality for database migration CLI commands.

**Features:**

- Implements `ApplicationCommand` for CLI integration
- Provides access to config, command line options, and data source configuration

### MigrationGenerator

CLI generator for creating migration files, registered as a service.

The generator creates timestamped migration files and automatically includes them in the main changelog. It supports action detection from migration names (e.g., `create_table`, `add_column_to_table`).

### CLI Scripts

The module provides convenience scripts like `db-migrate.groovy` that wrap the underlying `dbmUpdate` command. 


## Notes

**Refactoring History:**

In the 2024.x release, the database migration plugin was merged into the framework from a separate repository (grace-database-migration). This consolidation brought the plugin into the core framework alongside other plugins like Hibernate, MongoDB, and GORM.

The module includes a `syncScripts` task that copies scripts from `src/main/scripts` to `build/resources/main/META-INF/commands` during resource processing.

**Groovy DSL Support:**

The module provides a custom `GroovyChangeLogParser` that allows defining database changelogs using Groovy DSL instead of XML, making migrations more idiomatic for Groovy developers.
