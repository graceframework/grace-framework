<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="actuatorenv"/>
    <title><g:message code="actuator.env.label" default="Environment" /></title>
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
                    <h1>Environment</h1>

                    <div class="table-responsive">
                        <table id="actuator-env" class="table table-bordered">
                            <thead>
                                <tr>
                                    <th rowspan="2"><g:message code="actuator.propertySource.name.label" default="PropertySource Name" /></th>
                                    <th colspan="2"><g:message code="actuator.propertySource.properties.label" default="Properties" /></th>
                                </tr>
                                <tr>
                                    <th>Name</th>
                                    <th>Value</th>
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
    $.getJSON(ACTUATOR_ENDPOINT + '/env', function(data) {
        let result = [];
        let propertySources = data['propertySources'];
        for (let i = 0; i < propertySources.length; i++) {
            let propertySource = propertySources[i];
            let properties = propertySource.properties;
            let propertyRows = '';
            let propCount = 1;
            if (Object.keys(properties).length > 0) {
                for (let name in properties) {
                    let value = properties[name].value;
                    propertyRows += '<tr><td>' + name + '</td><td>' + value + '</td></tr>';
                    propCount++;
                }
            } else {
                propertyRows += '<tr><td></td><td></td></tr>';
                propCount++;
            }
            let propertySourceRows = '<tr><td rowspan="' + propCount + '">' + propertySource.name + '</td>';
            result.push(propertySourceRows);
            result.push(propertyRows);
            result.push('</tr>');
        }
        $("#actuator-env > tbody").html(result);
    });
});
</script>
</content>
</body>
</html>
