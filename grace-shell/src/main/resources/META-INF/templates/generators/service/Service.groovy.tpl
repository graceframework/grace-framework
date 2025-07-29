package $packageName

import grails.gorm.transactions.Transactional

@Transactional
class ${className}Service {
<% methods.each { method -> %>
    def $method() {
    }
<% } %>
}
