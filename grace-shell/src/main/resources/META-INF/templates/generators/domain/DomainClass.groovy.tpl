package $packageName

class $className {
<% attributes.each { name, type -> %>
    $type $name<% } %>

    static constraints = {
    }

}
