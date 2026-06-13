# Grace Boot Async

`grace-boot-async` is a Spring Boot auto-configuration module that provides asynchronous programming support for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's async functionality more focused and better integrated with Spring Boot's module structure.

The module depends on `grace-plugin-async`, which contains the core async functionality, and use `grace-async-rxjava3` for RxJava3 integration by default.

### Controller Async Support

`AsyncActionResultTransformer` - Handles `Promise` return types from controller actions by managing the Servlet 3.x `AsyncContext` lifecycle, releasing the container thread while background tasks execute.

`AsyncController` trait - Provides direct access to the Servlet 3.x `startAsync()` method for manual async control in controllers.

### Promise Decorators

`AsyncWebRequestPromiseDecorator` - Binds the `GrailsWebRequest` to the promise execution thread, ensuring request attributes and context are available in async operations.

### Auto-Configuration

`ControllersAsyncAutoConfiguration` - Spring Boot auto-configuration that registers the `AsyncActionResultTransformer` and `PromiseFactory` beans for servlet-based web applications.

### Integration with Async Libraries

Grace Async integrates with various asynchronous libraries and frameworks including GPars and RxJava. The `grace-boot-async` module specifically includes RxJava3 support through its dependency on `grace-async-rxjava3`.
