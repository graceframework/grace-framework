// A Groovy script file that configures the compiler, allowing extensive control over how the code is compiled.
// Please see the Groovy compiler customization builder documentation for more information about the compiler configuration DSL.
// https://docs.groovy-lang.org/latest/html/documentation/#compilation-customizers
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        source.ast.putNodeMetaData('PROJECT_NAME', '$projectName')
        source.ast.putNodeMetaData('PROJECT_TYPE', '$projectType')
        source.ast.putNodeMetaData('PROJECT_VERSION', '$projectVersion')
        source.ast.putNodeMetaData('PROJECT_DIR', '$projectDir')
        source.ast.putNodeMetaData('GRAILS_APP_DIR', '$projectDir/app')
    }
}