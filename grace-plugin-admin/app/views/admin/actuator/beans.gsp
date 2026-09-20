<html>
<head>
    <meta name="layout" content="admin" />
    <meta name="admin.active.section" content="admin.tabs.system"/>
    <meta name="admin.active.item" content="actuatorbeans"/>
    <title><g:message code="actuator.beans.label" default="Beans" /></title>
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
                    <h1>Beans</h1>

                    <div class="table-responsive">
                        <table id="actuator-beans" class="table table-bordered">
                            <thead>
                                <tr>
                                    <th><g:message code="actuator.context.name.label" default="Context" /></th>
                                    <th><g:message code="actuator.context.parent.label" default="Parent Context" /></th>
                                    <th><g:message code="actuator.bean.name.label" default="Bean Name" /></th>
                                    <th><g:message code="actuator.bean.aliases.label" default="Bean Aliases" /></th>
                                    <th><g:message code="actuator.bean.scope.label" default="Bean Scope" /></th>
                                    <th><g:message code="actuator.bean.type.label" default="Bean Type" /></th>
                                    <th><g:message code="actuator.bean.resource.label" default="Bean Resource" /></th>
                                    <th><g:message code="actuator.bean.dependencies.label" default="Bean Dependencies" /></th>
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
    $.getJSON(ACTUATOR_ENDPOINT + '/beans', function(data) {
        let result = [];
        let contexts = data['contexts'];
        for (let contextName in contexts) {
            let context = contexts[contextName];
            let beanCount = 1;
            let beanRows = '';
            for (let beanName in context.beans) {
                let bean = context.beans[beanName];
                beanRows += '<tr><td>' + beanName + '</td>';
                beanRows += '<td>' + bean.aliases.join(', ') + '</td>';
                beanRows += '<td>' + bean.scope + '</td>';
                beanRows += '<td>' + bean.type + '</td>';
                beanRows += '<td>' + bean.resource + '</td>';
                beanRows += '<td>' + bean.dependencies.join(', ') + '</td></tr>';
                beanCount++;
            }
            let contextRow = '<tr><td rowspan="' + beanCount  + '">' + contextName + '</td><td rowspan="' + beanCount + '">' + context.parentId + '</td></tr>';
            result.push(contextRow);
            result.push(beanRows);
        }
        $("#actuator-beans > tbody").html(result);
    });
});
</script>
</content>
</body>
</html>
