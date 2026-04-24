description('Load the seed data from db/seeds.groovy') {
    usage 'grace db:seed'
}

namespace 'db'
name 'seed'

console.addStatus 'Load the seed data from db/seeds.groovy'

def seeds = new File(BuildSettings.BASE_DIR, 'db/seeds.groovy')

if (!seeds.exists()) {
    console.error "File 'db/seeds.groovy' not found"
    return false
}

try {
    def arguments = ["-Pargs=db/seeds.groovy"]
    commandLine.systemProperties.each { key, value ->
        arguments << "-D${key}=$value".toString()
    }

    gradle."runScript"(*arguments)

    console.addStatus 'Load the seed data successfully.'
}
catch (Throwable e) {
    console.error "Failed to execute 'runScript' command", e
    return false
}

return true
