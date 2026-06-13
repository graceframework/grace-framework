# Grace Web-gsp

## Module Overview

The `grace-web-gsp` module is a web-specific integration layer for Groovy Server Pages (GSP) in the Grace Framework, providing Sitemesh layout support and JSP integration capabilities.

## Important APIs

### Core Rendering API

- **`GroovyPagesTemplateRenderer`** - High-level API for rendering GSP templates to a String or Writer

### Sitemesh Layout Integration

- **`GrailsContentBufferingResponse`** - Wraps the response to buffer GSP output for layout processing
- **`GrailsHTMLPageParser`** - Parses buffered HTML to extract head, body, and title sections
- **`GrailsLayoutDecoratorMapper`** - Maps requested pages to appropriate layout GSPs
- **`GrailsLayoutView`** - Specialized view for rendering Sitemesh layouts
- **`GSPSitemeshPage`** - Represents GSP content in Sitemesh format

### JSP Integration

- **`JspInvokeGrailsTagLibTag`** - Enables JSP tags to invoke Grails tag libraries

## How It Works

The module implements a content buffering and decoration pipeline for GSP rendering:

1. **Request Handling** - When a GSP is rendered, the response is wrapped in `GrailsContentBufferingResponse` to capture output
2. **Content Parsing** - `GrailsHTMLPageParser` extracts structured content (head, body, title) from the buffered HTML
3. **Layout Application** - `GrailsLayoutDecoratorMapper` determines the appropriate layout and `GrailsLayoutView` merges the content with the layout template
4. **Response Writing** - The final decorated content is written via `GrailsRoutablePrintWriter`

This enables the Sitemesh-based layout system where GSPs can specify layouts via `<meta name="layout" content="main"/>` tags.

## Usage in Other Modules

The `grace-web-gsp` module is used by several other Grace modules:

| Module | Dependency Type | Purpose |
|--------|----------------|---------|
| `grace-plugin-gsp` | api | Core GSP plugin integration |
| `grace-plugin-cache` | implementation | Cache tag library support |
| `grace-plugin-fields` | api | Form fields tag library rendering |
| `grace-plugin-controllers` | api | Controller view rendering support |
| `grace-plugin-rest` | compileOnly | Optional GSP support for REST applications |

## Notes

The module was consolidated from several other modules during the 2024.x refactoring, including `grace-web-sitemesh` and `grace-web-jsp`, to create a more cohesive web-specific GSP integration layer.
