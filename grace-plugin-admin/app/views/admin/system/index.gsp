<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="systeminfo"/>
    <title><g:message code="systemInfo.show.label" default="System Information"/></title>
</head>
<body>

    <div id="content" role="main">
        <div class="container">
            <section class="row">
                <a href="#list-system" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
                <div class="col-12" role="navigation">
                    <ul class="nav nav-pills">
                        <li class="nav-item">
                            <a class="nav-link" href="${createLink(uri: '/')}">
                                <i class="bi bi-house-fill"></i><g:message code="default.home.label"/>
                            </a>
                        </li>
                    </ul>
                </div>
            </section>
            <section class="row">
                <div id="list-system" class="col-12 scaffold scaffold-list" role="main">
                    <h1><g:message code="systemInfo.show.label" default="System Information" /></h1>

                    <table class="table table-bordered">
                        <tbody>
                            <tr>
                                <th><g:message code="systemInfo.appName.label" default="Application Name" /></th>
                                <td><g:meta name="info.app.name" default="Grace" /></td>
                            </tr>
                            <tr>
                                <th><g:message code="systemInfo.appVersion.label" default="Application Version" /></th>
                                <td><g:meta name="info.app.version" /></td>
                            </tr>
                            <tr>
                                <th><g:message code="systemInfo.grailsVersion.label" default="Grace Version" /></th>
                                <td><g:meta name="info.app.grailsVersion" /></td>
                            </tr>
                            <tr>
                                <th><g:message code="systemInfo.groovyVersion.label" default="Groovy Version" /></th>
                                <td>${GroovySystem.getVersion()}</td>
                            </tr>
                            <tr>
                                <th><g:message code="systemInfo.javaVersion.label" default="Java Version" /></th>
                                <td><%= System.properties['java.version']%> (<%= System.properties['java.vm.vendor']%>, <%= System.getProperty('java.vm.version')%>)</th>
                            </tr>
                            <tr>
                                <th><g:message code="systemInfo.environment.label" default="Environment" /></th>
                                <td>${grails.util.Environment.current.name}</td>
                            </tr>
                            <tr>
                                <th><g:message code="systemInfo.os.label" default="OS" /></th>
                                <td><%= System.properties['os.name']%> <%= System.properties['os.version']%> <%= System.properties['os.arch']%></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    </div>

</body>
</html>
