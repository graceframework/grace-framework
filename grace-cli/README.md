# Grace CLI

`grace-cli` is a foundational module that provides core APIs for the Grace command-line infrastructure, including command execution and code generation capabilities. It serves as the API layer for CLI functionality that is implemented by modules like `grace-shell`.

The `grace-shell` module depends on `grace-cli` to access the command and generator APIs, and implements the main CLI entry point `org.grails.cli.GrailsCli`. 

### Command API

`Command` - The base interface for CLI commands that can be executed by the Grace CLI.

`ApplicationCommand` - Allows you to create Gradle tasks in Grace projects, recommended over the older `GrailsApplicationCommand`. This was enhanced during the 2024.x refactoring to improve integration with Gradle.

### Generator API

`Generator` - The interface for code generators that provide the `generate` command for developers. This is the new way to provide code generation capabilities.

`AbstractGenerator` - Base class for generators that provides utility methods for file system operations, template rendering, and context management.

### Generation Context

`GenerationContext` - Provides context for code generation operations, including base directory, console for logging, and command-line arguments.
