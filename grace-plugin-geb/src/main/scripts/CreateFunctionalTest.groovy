description('Creates a Geb Functional Test') {
    usage "grace create-functional-test [Test Name]"
    argument name: 'Test Name', description: 'The name of the Functional Test'
	completer AllClassCompleter
	flag name: 'force', description: 'Whether to overwrite existing files'
}

boolean overwrite = flag('force')
model = model(args[0])
render template: 'FunctionalSpec.groovy',
	   destination: file("src/integration-test/groovy/$model.packagePath/${model.simpleName}Spec.groovy"),
	   model: model,
	   overwrite: overwrite
