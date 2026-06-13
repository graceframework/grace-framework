# Grace Boot MongoDB

`grace-boot-mongodb` is a Spring Boot auto-configuration module that provides MongoDB document database integration through GORM (Grails Object Relational Mapping) for Grace applications.

This module serves as a Spring Boot starter for MongoDB/GORM functionality, wrapping the MongoDB plugin which was merged into the framework during the 2024.x refactoring.
Grace Data MongoDB provides a GORM implementation for the MongoDB Document Database, bridging the gap between key-value stores and traditional RDBMS systems. The module supports combined use of MongoDB and Hibernate in the same application, with entity routing based on the `mapWith` property.

### Auto-Configuration

`MongoDbGormAutoConfiguration` - The central Spring Boot auto-configuration class that:

- Runs after Spring Boot's `MongoAutoConfiguration` with order 300
- Creates the `MongoDatastore` bean by aggregating domain classes from `GrailsApplication` and scanning for `@Entity` annotated classes
- Filters domain classes based on `mapWith` property - only classes with `mapWith = "mongo"` are routed to the MongoDB datastore when multiple datastores are present
- Registers GORM services as Spring beans automatically

### Registered Beans

The auto-configuration registers the following beans:

- `MongoMappingContext` - The GORM mapping context for MongoDB
- `AutoTimestampEventListener` - Handles automatic timestamp updates
- `DatastorePersistenceContextInterceptor` - Manages the MongoDB persistence context
- `PersistenceContextInterceptorAggregator` - Coordinates multiple persistence contexts
- `transactionManager` (aliased as `mongoTransactionManager`) - The MongoDB transaction manager
- `mongoOpenSessionInViewInterceptor` - Binds MongoDB sessions to web requests

### Plugin Definition

`MongodbGrailsPlugin` - The Grace plugin that provides integration between Grace and MongoDB document datastore through GORM API. It observes domain class artefacts to respond to changes in the domain model.
