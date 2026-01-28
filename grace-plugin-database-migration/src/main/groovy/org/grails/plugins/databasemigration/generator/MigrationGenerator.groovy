/*
 * Copyright 2022-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins.databasemigration.generator

import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import groovy.transform.CompileStatic

import grails.cli.generator.AbstractGenerator

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
class MigrationGenerator extends AbstractGenerator {

    @Override
    boolean generate() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return false
        }

        boolean overwrite = commandLine.hasOption('force') || commandLine.hasOption('f')

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of('UTC'))
        String migrationNumber = now.format(DateTimeFormatter.ofPattern('yyyyMMddHHmmss'))
        String migrationName = args[1].uncapitalize()
        String migrationFileName = [migrationNumber, migrationName].join('_') + '.groovy'

        File changelogFile = new File(baseDir, 'db/migrations/changelog.groovy')
        Map<String, Object> model = new HashMap<>()
        model.put('id', System.currentTimeMillis().toString())

        String migrationFile = 'db/migrations/' + migrationFileName
        createFile('Migration.groovy.tpl', migrationFile, model, overwrite)

        if (changelogFile.exists()) {
            String content = changelogFile.text
            changelogFile.text = content.substring(0, content.length() - 3) +
                    "\n    include file: '${migrationFileName}'\n}\n"
        }

        true
    }

}
