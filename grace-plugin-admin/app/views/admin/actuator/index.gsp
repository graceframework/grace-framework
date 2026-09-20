<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="actuatorindex"/>
    <title><g:message code="actuator.endpoints.label" default="Endpoints" /></title>
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
                    <h1>Endpoints</h1>

                    <table id="actuator-links" class="table table-bordered">
                        <thead>
                            <tr>
                                <th>&nbsp;</th>
                                <th><g:message code="actuator.name.label" default="Name" /></th>
                                <th><g:message code="actuator.url.label" default="URL" /></th>
                                <th><g:message code="actuator.templated.label" default="Templated" /></th>
                            </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    </div>
<content tag="scripts">
<script type="text/javascript">
let ACTUATOR_ENDPOINT = "${grailsApplication.config.getProperty('management.endpoints.web.base-path', String, '/actuator')}";
$(function() {
    $.getJSON(ACTUATOR_ENDPOINT, function(data) {
        let endpoints = [];
        let index = 0;
        let links = data._links;
        for (let link in links) {
            let endpoint = '<tr><td>' + (index + 1) + '</td><td>' + link + '</td><td>' + links[link].href + '</td><td>' + links[link].templated + '</td></tr>';
            endpoints.push(endpoint);
            index++;
        }
        $("#actuator-links > tbody").html(endpoints);
    });
});
</script>
</content>
</body>
</html>
