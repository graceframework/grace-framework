package $packageName

class ${className} {
<% classAttributes.each { name, type -> %>
    $type $name<% } %>

    static constraints = {
    }

}
