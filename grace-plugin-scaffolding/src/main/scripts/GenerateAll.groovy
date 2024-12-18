import org.grails.cli.interactive.completers.DomainClassCompleter

description("Generates a Controller that performs CRUD operations and the associated views") {
    usage "grace generate-all [DOMAIN CLASS]"
    argument name: 'Domain Class', description: "The name of the Domain Class", required: true
    completer DomainClassCompleter
    flag name: 'force', description: "Whether to overwrite existing files"
}

if (args) {
    generateController(*args)
    generateViews(*args)
} else {
    error "No domain class specified"
}
