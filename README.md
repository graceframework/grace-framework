[![Grace CI](https://github.com/graceframework/grace-scaffolding/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-scaffolding/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework/grace-scaffolding-core.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://twitter.com/graceframework)

[![Groovy Version](https://img.shields.io/badge/Groovy-4.0.24-blue?style=flat&color=4298b8)](https://groovy-lang.org/releasenotes/groovy-4.0.html)
[![Grace Version](https://img.shields.io/badge/Grace-2023.3.0-blue?style=flat&color=f49b06)](https://github.com/graceframework/grace-framework/releases/tag/v2023.3.0-M1)
[![Spring Boot Version](https://img.shields.io/badge/Spring_Boot-3.3.7-blue?style=flat&color=6db33f)](https://github.com/spring-projects/spring-boot/releases/tag/v3.3.7)

# Grace Scaffolding

Grace Scaffolding and [Fields](https://github.com/graceframework/grace-fields) plugin work together will make you more productive.

Grace Scaffolding lets you generate some basic CRUD interfaces for a domain class, including:

* GSP views

* Controller actions for create/read/update/delete (CRUD) operations

you can set the `scaffold` property in the Controller to a specific domain class to use Dynamic scaffolding:

```groovy
class BookController {
    static scaffold = Book
}
```

Grace CLI provides some useful commands to do this job quickly,

```bash
$ grace generate-all Book
$ grace generate-async-controller Book
$ grace generate-controller Book
$ grace generate-views Book
$ grace create-scaffold-controller Book
```

## Versions

To make it easier for users to use and upgrade, Plugin adopts a version policy consistent with the [Grace Framework](https://github.com/graceframework/grace-framework).

| Plugin Version | Grace Version |
|----------------|---------------|
| 6.3.x          | 2023.3.x      |
| 6.2.x          | 2023.2.x      |
| 6.1.x          | 2023.1.x      |
| 6.0.x          | 2023.0.x      |
| 5.2.x          | 2022.2.x      |
| 5.1.x          | 2022.1.x      |
| 5.0.x          | 2022.0.x      |

## License

This plugin is available as open source under the terms of the [APACHE LICENSE, VERSION 2.0](http://apache.org/Licenses/LICENSE-2.0)

## Links

- [Grace Framework](https://github.com/graceframework/grace-framework)
- [Grace Plugins](https://github.com/grace-plugins)
