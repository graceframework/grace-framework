# Grace Docs

`grace-docs` is a module that provides the documentation generation infrastructure for Grace Framework, transforming structured content (primarily AsciiDoc) into HTML and PDF outputs. It contains the core documentation engine and is integrated into the build system via the `GrailsDocGradlePlugin`.


### Core Documentation Components

`DocPublisher` - The central coordinator of the documentation process that manages the lifecycle of documentation generation, including initializing the environment, unpacking resources, and invoking specific rendering engines. It supports multiple rendering engines including the legacy `DocEngine` (Radeox-based) and the modern `AsciiDocEngine`.

`PdfBuilder` - Handles PDF generation using `ITextRenderer` (Flying Saucer) and `ITextFontResolver` to convert generated HTML into PDF documents. It performs HTML cleanup using `Jsoup` and supports custom CSS and font embedding for international characters.

`DocEngine` - Legacy Radeox-based documentation engine that still exists for backward compatibility.

### Gradle Plugin Integration

`GrailsDocGradlePlugin` - The Gradle plugin that exposes the documentation system to the build environment by registering the `publishGuide` task.

`PublishGuideTask` - The primary entry point for generating documentation that configures the `DocPublisher` with project-specific information such as version, title, and source directories. It automatically detects and includes API documentation (Javadoc) and Groovydoc into the final manual structure.
