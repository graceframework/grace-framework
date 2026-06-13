# Grace Boot Logging

`grace-boot-logging` is a Spring Boot auto-configuration module that provides logging functionality for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's logging functionality more focused and better integrated with Spring Boot's module structure.

The module depends on `grace-logging` (which contains the core logging functionality) and Spring Boot's logging starter.

The logging capabilities are primarily provided through the `grace-logging` module, which `grace-boot-logging` wraps. The module integrates with Spring Boot's logging infrastructure via the `spring-boot-starter-logging` dependency.
