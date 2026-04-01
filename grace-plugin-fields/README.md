## Grace Plugin Fields 

This plugin attempts to achieve that by using GSP templates looked up by convention.
Developers can then create templates for rendering particular properties or types of properties with the former overriding the latter.

This plugin allows you to customize the rendering of input fields for properties of domain objects, command beans and POGOs based on their type, name, etc.

This plugin aims to:

* Use good defaults for fields.
* Make it very easy to override the field rendering for particular properties or property types without having to replace entire form templates.
* Not require you to copy and paste markup for containers, labels and error messages just because you need a different input type.
* Support inputs for property paths of arbitrary depth and with indexing.
* Enable other plugins to provide field rendering for special property types that gets picked up automatically (e.g. the Joda Time plugin can provide templates for the various date/time types).
* Support embedded properties of GORM domain classes.
