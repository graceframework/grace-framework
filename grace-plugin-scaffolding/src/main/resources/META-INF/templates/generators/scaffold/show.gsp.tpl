<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="\${message(code: '${propertyName}.label', default: '${className}')}" />
        <title><g:message code="default.show.label" args="[entityName]" /></title>
    </head>
    <body>
    <div id="content" role="main">
        <div class="container">
            <section class="row">
                <a href="#show-${propertyName}" class="skip" tabindex="-1"><g:message code="default.link.skip.label" /></a>
                <div class="col-12" role="navigation">
                    <ul class="nav nav-pills">
                        <li class="nav-item">
                            <a class="nav-link" href="\${createLink(uri: '/')}">
                                <i class="bi bi-house-fill"></i><g:message code="default.home.label" />
                            </a>
                        </li>
                        <li class="nav-item">
                            <g:link class="nav-link" action="index">
                                <i class="bi bi-journals"></i><g:message code="default.list.label" args="[entityName]" />
                            </g:link>
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
                <div id="show-${propertyName}" class="col-12 scaffold scaffold-show" role="main">
                    <h1><g:message code="default.show.label" args="[entityName]" /></h1>
                    <g:if test="\${flash.message}">
                    <div class="alert alert-success" role="status"><i class="bi bi-info-circle"></i>\${flash.message}</div>
                    </g:if>
                    <f:display bean="${propertyName}" />
                    <g:form resource="\${this.${propertyName}}" method="DELETE">
                        <fieldset class="buttons offset-3">
                            <g:link class="btn btn-primary" action="edit" resource="\${this.${propertyName}}">
                                <i class="bi bi-journal-text"></i><g:message code="default.button.edit.label" default="Edit" />
                            </g:link>
                            <button class="btn btn-danger" onclick="return confirm('\${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');">
                                <i class="bi bi-journal-x"></i><g:message code="default.button.delete.label" default="Delete" />
                            </button>
                        </fieldset>
                    </g:form>
                </div>
            </section>
        </div>
    </div>
    </body>
</html>
