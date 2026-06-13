# Grace Boot Test

`grace-boot-test` is a Spring Boot auto-configuration module that provides testing support for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's testing functionality more focused and better integrated with Spring Boot's module structure.

The module depends on `grace-test` (core testing functionality), `grace-test-support` (testing support utilities), and Spring Boot's test starter.

The testing capabilities are primarily provided through the dependencies that `grace-boot-test` aggregates:

- `grace-test` - Core testing framework for Grace applications
- `grace-test-support` - Testing support utilities and helpers
- `spring-boot-starter-test` - Spring Boot's testing starter with JUnit, Mockito, and other testing libraries
