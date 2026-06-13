# Grace Plugin Fields 

The `grace-plugin-fields` module is a Grace Framework plugin that provides customizable form-field rendering based on overrideable GSP templates, allowing developers to customize the rendering of input fields for properties of domain objects, command beans, and POGOs based on their type, name, and other characteristics.


## Module Overview

The `grace-plugin-fields` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-plugin-databinding`, `grace-plugin-taglibs`, `grace-plugin-validation`, `grace-scaffolding-core`, `grace-web`, and `grace-web-gsp`.  

## Important APIs

### FieldsGrailsPlugin

The main plugin class `FieldsGrailsPlugin` extends `Plugin` and provides the fields functionality.

**Key Features:**

- Loads after the `domainClass` plugin
- Provided Artefacts: `FormFieldsTagLib`

### FormFieldsTagLib

The tag library that provides the form field rendering functionality, registered as a provided artefact by the plugin.

### Renderers

The plugin includes several renderer classes for handling markup generation:

- `ContextMarkupRenderer` and `ContextMarkupRendererImpl`
- `DomainMarkupRendererImpl`
- `DelegatingBeanPropertyAccessorImpl`

### CLI Commands

The module provides an `install` command to copy templates to `app/views`:

```
grace fields:install
```

## Notes

**Refactoring History:**

In the 2024.x release, the fields plugin was merged into the framework from a separate repository as part of the core plugins consolidation effort.

**Design Goals:**

The plugin aims to:

- Use good defaults for fields
- Make it very easy to override field rendering for particular properties or property types without having to replace entire form templates
- Not require copying and pasting markup for containers, labels, and error messages just because you need a different input type
- Support inputs for property paths of arbitrary depth and with indexing
- Enable other plugins to provide field rendering for special property types that gets picked up automatically
- Support embedded properties of GORM domain classes
