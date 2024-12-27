<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="\${message(code: '${propertyName}.label', default: '${className}')}" />
        <title><g:message code="default.list.label" args="[entityName]" /></title>
    </head>
    <body>
    <div id="content" role="main">
        <div class="container">
            <section class="row">
                <a href="#list-${propertyName}" class="skip" tabindex="-1"><g:message code="default.link.skip.label" /></a>
                <div class="col-12" role="navigation">
                    <ul class="nav nav-pills">
                        <li class="nav-item">
                            <a class="nav-link" href="\${createLink(uri: '/')}">
                                <i class="bi bi-house-fill"></i><g:message code="default.home.label" />
                            </a>
                        </li>
                        <li class="nav-item">
                            <g:link class="nav-link" action="create">
                                <i class="bi bi-journal-plus"></i><g:message code="default.new.label" args="[entityName]" />
                            </g:link>
                        </li>
                    </ul>
                </div>
            </section>
            <section class="row">
                <div id="list-${propertyName}" class="col-12 scaffold scaffold-list" role="main">
                    <h1><g:message code="default.list.label" args="[entityName]" /></h1>
                    <g:if test="\${flash.message}">
                        <div class="alert alert-success" role="status"><i class="bi bi-info-circle"></i>\${flash.message}</div>
                    </g:if>
                    <f:table collection="\${${propertyName}List}" />

                    <g:if test="\${${propertyName}Count > params.int('max')}">
                    <div class="pagination justify-content-center">
                        <g:paginate total="\${${propertyName}Count ?: 0}" />
                    </div>
                    </g:if>
                </div>
            </section>
        </div>
    </div>
    </body>
</html>