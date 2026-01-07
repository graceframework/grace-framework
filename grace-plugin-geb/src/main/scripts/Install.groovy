import grails.plugin.geb.GebGrailsPlugin

namespace 'geb'
description "Install Geb", "grace geb:install"
visible false

URL gebConfig = GebGrailsPlugin.getResource('/META-INF/templates/GebConfig.groovy')
File projectDir = executionContext.baseDir
File resourcesDir = new File(projectDir, 'src/integration-test/resources')
File gebFile = new File(resourcesDir, 'GebConfig.groovy')
File buildFile = new File(projectDir, 'build.gradle')

if (!resourcesDir.exists()) {
    resourcesDir.mkdir()
}

use(FileCategory) {
    // Copy GebConfig.groovy to 'src/integration-test/resources'
    gebFile << gebConfig

    // Set system properties in the build.gradle
    buildFile.insertAfter 'useJUnitPlatform()', '''
    systemProperty 'geb.env', System.getProperty('geb.env')
    systemProperty 'geb.build.reportsDir', reporting.baseDirectory.file('geb/integrationTest')'''
}


class FileCategory {

    def static leftShift(File file, URL url) {
        url.withInputStream { is ->
            file.withOutputStream { os ->
                def bs = new BufferedOutputStream(os)
                bs << is
            }
        }
    }

    def static leftShift(File dest, File src) {
        src.withInputStream { is ->
            dest.withOutputStream { os ->
                def bs = new BufferedOutputStream(os)
                bs << is
            }
        }
    }

    def static insertAfter(File file, String searchString, String text) {
        if (!file?.exists()) return
        String content = file.text
        if (content?.indexOf(searchString) <= 0) return
        String before = content.takeBefore(searchString)
        String after = content.takeAfter(searchString)
        StringBuffer newContent = new StringBuffer()
        newContent << before << searchString << text << after
        file.text = newContent
    }

    def static insertBefore(File file, String searchString, String text) {
        if (!file?.exists()) return
        String content = file.text
        if (content?.indexOf(searchString) <= 0) return
        String before = content.takeBefore(searchString)
        String after = content.takeAfter(searchString)
        StringBuffer newContent = new StringBuffer()
        newContent << before << text << searchString << after
        file.text = newContent
    }
}

consoleLogger.addStatus "Copying 'GebConfig.groovy' to 'src/integration-test/resources'"
