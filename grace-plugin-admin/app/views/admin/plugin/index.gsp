<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="pluginlist"/>
    <title><g:message code="default.plugins.label" default="Plugins" /></title>
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
                    <h1>Plugins: Total ${pluginList.size()} Installed</h1>

                    <table class="table table-bordered">
                        <thead>
                            <tr>
                                <th>&nbsp;</th>
                                <th><g:message code="plugin.name.label" default="Name" /></th>
                                <th><g:message code="plugin.version.label" default="Version" /></th>
                                <th><g:message code="plugin.description.label" default="Description" /></th>
                                <th><g:message code="plugin.enabled.label" default="Enabled" /></th>
                            </tr>
                        </thead>
                        <tbody>
                            <g:each in="${pluginList}" status="i" var="plugin">
                                <tr>
                                    <td>${i + 1}</td>
                                    <td>${plugin.name}</td>
                                    <td width="15%" >${plugin?.version}</td>
                                    <td>${plugin?.properties.description}</td>
                                    <td><g:formatBoolean boolean="${plugin.isEnabled()}" true="Y" false="N" /></td>
                                </tr>
                            </g:each>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    </div>

</body>
</html>
