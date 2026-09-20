<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="actuatorloggers"/>
    <title><g:message code="actuator.loggers.label" default="Loggers" /></title>
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
                    <h1>Loggers</h1>

                    <div class="table-responsive">
                        <table id="actuator-loggers" class="table table-bordered">
                            <thead>
                                <tr>
                                    <th><g:message code="actuator.loggers.name.label" default="Logger Name" /></th>
                                    <th><g:message code="actuator.loggers.level.label" default="Level" /></th>
                                </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>
        </div>
    </div>

<content tag="scripts">
<script type="text/javascript">
let ACTUATOR_ENDPOINT = "${grailsApplication.config.getProperty('management.endpoints.web.base-path', String, '/actuator')}";
$(function() {
    $.getJSON(ACTUATOR_ENDPOINT + '/loggers', function(data) {
        let result = [];
        let items = '';
        let loggers = data['loggers'];
        for (let log in loggers) {
            let level = loggers[log].effectiveLevel;
            let levelClass;
            items += '<tr><td>' + log + '</td>';
            if (level === 'INFO') {
                levelClass = 'text-success';
            } else if (level === 'DEBUG') {
                levelClass = 'text-info';
            } else if (level === 'WARN') {
                levelClass = 'text-warning';
            } else if (level === 'ERROR') {
                levelClass = 'text-danger';
            } else {
                levelClass = 'text-secondary';
            }
            items += '<td><span class="' + levelClass + '">' + level + '</span></td></tr>';
        }
        result.push(items);
        $("#actuator-loggers > tbody").html(result);
    });
});
</script>
</content>
</body>
</html>
