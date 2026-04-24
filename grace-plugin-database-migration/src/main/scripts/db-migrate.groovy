description('Migrates the database') {
    usage 'grace db:migrate'
}

namespace 'db'
name 'migrate'

console.addStatus 'Migrates the database ...'

try {
    def arguments = ["dbmUpdate", "-Pargs=${commandLine.remainingArgs.join(' ')}"]
    commandLine.systemProperties.each { key, value ->
        arguments << "-D${key}=$value".toString()
    }

    gradle."runCommand"(*arguments)
}
catch (Throwable e) {
    console.error "Failed to execute 'runCommand' command", e
    return false
}

return true
