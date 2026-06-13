# Grace Plugin Datasource

The `grace-plugin-datasource` module is a Grace Framework plugin that provides DataSource configuration and management capabilities, integrating with Spring Boot's DataSource auto-configuration and supporting multiple data sources through GORM's ConnectionSources abstraction.


## Module Overview

The `grace-plugin-datasource` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-core` and `grace-plugin-api`. 

It has API dependencies on HikariCP, Grace Datastore core and GORM, Groovy SQL, Spring Boot autoconfigure, Spring context, and Spring JDBC.

## Important APIs

### DataSourceGrailsPlugin

The main plugin class is `DataSourceGrailsPlugin`, which registers the datasource functionality with the Grace framework.

### DataSourcePluginConfiguration

Spring Boot auto-configuration class that provides DataSource integration beans.

**Configuration:**

- Runs before `DataSourceAutoConfiguration` and `SqlInitializationAutoConfiguration`
- Auto-configure order 100
- Conditional on `GrailsDataSourceCondition` which checks for `dataSource.url` or `dataSources` properties

**Provided Beans:**

- `dataSourceConnectionSources` - `ConnectionSources<DataSource, DataSourceSettings>` for managing multiple data sources
- `dataSource` - Primary DataSource bean (marked with @Primary)
- `embeddedDatabaseShutdownHook` - For graceful shutdown of embedded databases

**BeanPostProcessorsRegistrar:**

Registers additional DataSource beans for non-default connection sources, allowing multiple named data sources to be accessed as Spring beans.
