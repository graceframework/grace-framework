import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import grails.plugin.formfields.FieldsGrailsPlugin

namespace 'fields'
description "Install Fields", "grace fields:install"
visible false

URL url = FieldsGrailsPlugin.getResource('/META-INF/templates/_fields')
File projectDir = executionContext.baseDir
File viewsDir = new File(projectDir, 'app/views')
File jarFile = new File(new URI(url.file.takeBefore('!')))

use(FileCategory) {
    jarFile.unzip(viewsDir, 'META-INF/templates/')
}

consoleLogger.addStatus "Copying the default templates to 'app/views'"


class FileCategory {

    def static unzip(File zipFile, File distDir, String path) {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))
        ZipEntry entry

        while (entry = zis.nextEntry) {
            String fileName = entry.name
            if (!fileName.startsWith(path)) {
                continue
            }
            File file = new File(distDir, fileName - path)
            if (fileName.endsWith('/')) {
                if (file.isFile()) {
                    file.delete()
                }
                file.mkdirs()
            }
            else {
                Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        zis.closeEntry()
        zis.close()
    }

}