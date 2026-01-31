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
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import grails.cli.generator.AbstractGenerator

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
class MigrationGenerator extends AbstractGenerator {

    private static final Pattern ADD = Pattern.compile("^(add)_.*_to_(.*)")
    private static final Pattern CREATE = Pattern.compile("^(create)_(.+)")
    private static final Pattern REMOVE = Pattern.compile("^(remove)_.*?_from_(.*)")
    private static final Pattern JOIN = Pattern.compile(".*_(join)_table_(.*)")
    private static final Map<String, String> DATA_TYPES = [
            'string': 'varchar(50)',
            'String': 'varchar(50)',
            'int': 'int',
            'integer': 'int',
            'Integer': 'int',
            'long': 'bigint',
            'Long': 'bigint',
            'date': 'datetime',
            'boolean': 'boolean',
            'Boolean': 'boolean'
    ]

    @Override
    boolean generate() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            console.error('Missing the name of migration file!')
            return false
        }

        String filename = args[1]
        if (!(filename ==~ /^[-_a-zA-Z0-9]+$/)) {
            console.error("Illegal name for migration file: $filename.")
            return false
        }

        boolean overwrite = commandLine.hasOption('force') || commandLine.hasOption('f')

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of('UTC'))
        String migrationNumber = now.format(DateTimeFormatter.ofPattern('yyyyMMddHHmmss'))
        String migrationName = toSnakeCase(filename)
        String migrationFileName = [migrationNumber, migrationName].join('_') + '.groovy'

        File changelogFile = new File(baseDir, 'db/migrations/changelog.groovy')
        Map<String, Object> model = new HashMap<>()
        model.put('id', System.currentTimeMillis().toString())
        model.put('author', getAuthor())

        String tableName = ''
        String migrationAction = ''
        List matches = getTableAndActionName(migrationName)
        if (matches) {
            tableName = matches[1]
            migrationAction = matches[0]
            if (migrationAction == 'join') {
                model.put('joinTables', args.size() > 3 ? args[2..-1] : tableName.toLowerCase().split('_'))
            }
            else {
                Map<String, String> tableColumns = new LinkedHashMap<>()
                String[] columns = (args.size() >= 3 ? args[2..-1] : []) as String[]
                columns.each { String item ->
                    String[] attr = (item.contains(':') ? item.split(':') : [item, 'string']) as String[]
                    tableColumns[attr[0]] = attr[1]
                }
                model['tableColumns'] = tableColumns
            }
        }
        model.put('tableName', tableName)
        model.put('migrationAction', migrationAction)

        String migrationFile = 'db/migrations/' + migrationFileName
        createFile('Migration.groovy.tpl', migrationFile, model, overwrite)

        if (changelogFile.exists()) {
            String content = changelogFile.text
            changelogFile.text = content.substring(0, content.length() - 3) +
                    "\n    include file: '${migrationFileName}'\n}\n"
        }

        true
    }

    private String toSnakeCase(String str) {
        StringBuilder result = new StringBuilder()
        char firstChar = str.charAt(0)
        result.append(Character.toLowerCase(firstChar))

        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i)
            if (ch == '-') {
                result.append('_')
            }
            else if (Character.isUpperCase(ch)) {
                if (str.charAt(i-1) != '_') {
                    result.append('_')
                }
                result.append(Character.toLowerCase(ch))
            }
            else {
                result.append(ch)
            }
        }
        return result.toString()
    }

    private String getAuthor() {
        loadApplicationConfig().getProperty('dataSource.username') ?: System.getProperty('user.name')
    }

    @CompileDynamic
    private List getTableAndActionName(String filename) {
        for (Pattern it in [ADD, REMOVE, JOIN, CREATE]) {
            Matcher matcher = it.matcher(filename)
            if (matcher.matches()) {
                return matcher[0][1..-1]
            }
        }
        []
    }

}
