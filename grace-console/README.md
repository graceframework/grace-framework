# Grace Console

`grace-console` is a module that provides a REPL-like environment for Grace applications, enabling developers to interactively test code, query databases via GORM, and debug applications. It leverages Groovy's native console and shell capabilities integrated with Grace's Spring Boot context.

### Console Command Integration

`ConsoleCommand` - A CLI command in `grace-shell` that launches the Grace interactive console by invoking the Gradle `console` task. It supports options like `--debug-jvm` for remote debugging and `--verbose` for detailed output.

### Enhanced Runners

During the 2024.x refactoring, the console module was enhanced with:

- `GrailsApplicationCommandRunner` - Rewritten using `ApplicationRunner` to support executing GORM methods in commands
- `GrailsApplicationScriptRunner` - Rewritten using `ApplicationRunner` for script execution
