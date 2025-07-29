package $packageName

class ${className}TagLib {
    static defaultEncodeAs = [taglib:'html']
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]
<% tags.each { tag -> %>
    def $tag = { attrs, body ->
    }
<% } %>
}
