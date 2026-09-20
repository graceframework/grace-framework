<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="actuatorinfo"/>
    <title><g:message code="actuator.info.label" default="Info" /></title>
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
                <h1>Info</h1>

                <table id="actuator-info" class="table table-bordered">
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
    $.getJSON(ACTUATOR_ENDPOINT + '/info', function(data) {
        let infos = [];
        let app = data['app'];
        for (let name in app) {
            let info = '<tr><th>' + name + '</th><td>' + app[name] + '</td></tr>';
            infos.push(info);
        }
        $("#actuator-info > tbody").html(infos);
    });
});
</script>
</content>
</body>
</html>
