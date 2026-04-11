package $packageName

class ${className}TagLib {

    static defaultEncodeAs = [taglib: 'html']
<% tags.each { tag -> %>
    def $tag = { attrs, body ->
    }
<% } %>
}
