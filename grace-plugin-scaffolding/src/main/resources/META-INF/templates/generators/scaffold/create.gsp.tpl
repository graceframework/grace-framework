<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="\${message(code: '${propertyName}.label', default: '${className}')}" />
        <title><g:message code="default.create.label" args="[entityName]" /></title>
    </head>
    <body>
    <div id="content" role="main">
        <div class="container">
            <section class="row">
                <a href="#create-${propertyName}" class="skip" tabindex="-1"><g:message code="default.link.skip.label" /></a>
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
                    </ul>
                </div>
            </section>
            <section class="row">
                <div id="create-${propertyName}" class="col-12 scaffold scaffold-create" role="main">
                    <h1><g:message code="default.create.label" args="[entityName]" /></h1>
                    <g:if test="\${flash.message}">
                    <div class="alert alert-success" role="status"><i class="bi bi-info-circle">\${flash.message}</div>
                    </g:if>
                    <g:hasErrors bean="\${this.${propertyName}}">
                    <div class="alert alert-danger" role="alert">
                        <ul class="errors">
                            <g:eachError bean="\${this.${propertyName}}" var="error">
                            <li <g:if test="\${error in org.springframework.validation.FieldError}">data-field-id="\${error.field}"</g:if>>
                                <i class="bi bi-exclamation-circle"></i><g:message error="\${error}"/>
                            </li>
                            </g:eachError>
                        </ul>
                    </div>
                    </g:hasErrors>
                    <g:form resource="\${this.${propertyName}}" method="POST">
                        <fieldset class="form">
                            <f:all bean="${propertyName}"/>
                        </fieldset>
                        <fieldset class="buttons offset-3">
                            <button name="create" class="btn btn-primary">
                                <i class="bi bi-journal-plus"></i>
                                <g:message code="default.button.create.label" default="Create" />
                            </button>
                        </fieldset>
                    </g:form>
                </div>
            </section>
        </div>
    </div>
    </body>
</html>
