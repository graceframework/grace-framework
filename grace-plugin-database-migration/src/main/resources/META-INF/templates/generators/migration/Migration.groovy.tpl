databaseChangeLog = {

    changeSet(author: "$author", id: "$id-1") {
        <% if (migrationAction == 'create') { %>createTable(tableName: "$tableName") {
<% tableColumns.each { name, type -> %>
            column(name: "$name", type: "$type")
<% } %>
        }<% } else if (migrationAction == 'add') { %>addColumn(tableName: "$tableName") {
<% tableColumns.each { name, type -> %>
            column(name: "$name", type: "$type")
<% } %>
        }<% } else if (migrationAction == 'remove') { %>dropColumn(tableName: "$tableName") {
<% tableColumns.each { name, type -> %>
            column(name: "$name")
<% } %>
        }<% } else if (migrationAction == 'join') { %>createTable(tableName: "$tableName") {
<% joinTables.each { t -> %>
            column(name: "${t}_id", type: "BIGINT") {
                constraints(referencedTableName: "$t", referencedColumnNames: "id", foreignKeyName: "FK_${t}_id", nullable: "false")
            }
<% } %>
        }<% } %>
    }

}
