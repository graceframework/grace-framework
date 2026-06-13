# Grace Boot Hibernate

`grace-boot-hibernate` is a Spring Boot auto-configuration module that provides SQL database integration through Hibernate and GORM (Grails Object Relational Mapping) for Grace applications.

This module serves as a Spring Boot starter for Hibernate/GORM functionality, wrapping the Hibernate plugin which was merged into the framework during the 2024.x refactoring.
The compiler auto-configuration automatically adds this module as a dependency when Hibernate classes are detected in the application. Grace Data Hibernate provides a GORM implementation for Hibernate ORM and has been migrated to the Jakarta namespace, supporting [Hibernate 5.6](https://hibernate.org/orm/documentation/5.6/).

[Grace Data Hibernate](https://github.com/graceframework/grace-data-hibernate) is the original implementation of GORM and has evolved dramatically over the years from a few Meta-programming functions into a complete data access framework with multiple implementations for different datastores relational and NoSQL.

### Auto-Configuration

`HibernateGormAutoConfiguration` - The central Spring Boot auto-configuration class that:

- Runs after `DataSourceAutoConfiguration` and before `HibernateJpaAutoConfiguration`
- Creates the `HibernateDatastore` bean by aggregating domain classes from `GrailsApplication` and scanning for `@Entity` annotated classes
- Registers `SessionFactory`, `HibernateMappingContext`, and `PlatformTransactionManager` as primary beans
- Configures the Open Session in View interceptor for web applications (conditional on `hibernate.osiv.enabled=true`)

### Plugin Definition

`HibernateGrailsPlugin` - The Grace plugin that provides integration between Grace and Hibernate through GORM. It observes domain classes and configures conversion services for Hibernate.

### Session Management

The module provides several persistence context interceptors for managing Hibernate sessions:

- `AggregatePersistenceContextInterceptor` - Coordinates session management across multiple data sources
- `GrailsOpenSessionInViewInterceptor` - Binds Hibernate sessions to web requests to prevent `LazyInitializationException`
