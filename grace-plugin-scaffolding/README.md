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
