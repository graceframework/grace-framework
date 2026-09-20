<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="actuatorhealth"/>
    <title><g:message code="actuator.health.label" default="Health" /></title>
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
                    <h1>Health</h1>

                    <table id="actuator-health" class="table table-bordered">
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
    $.getJSON(ACTUATOR_ENDPOINT + '/health', function(data) {
        let result = [];
        for (let name in data) {
            let health = data[name];
            let cssClass;
            if (health === 'UP') {
                cssClass = 'text-success';
            } else {
                cssClass = 'text-danger';
            }
            let item = '<tr><th>' + name + '</th><td><span class="' + cssClass + '">' + data[name] + '</span></td></tr>';
            result.push(item);
        }
        $("#actuator-health > tbody").html(result);
    });
});
</script>
</content>
</body>
</html>
