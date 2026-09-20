<!doctype html>
<html lang="en" class="no-js">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>
        <g:layoutTitle default="Grace Admin"/>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <asset:link rel="icon" href="favicon.ico" type="image/x-ico"/>

    <asset:stylesheet src="bootstrap.css"/>
    <asset:stylesheet src="@grace/admin.css"/>

    <g:layoutHead/>
</head>

<body>

<nav class="navbar navbar-expand-lg navbar-dark bg-dark fixed-top" role="navigation">
    <div class="container-fluid">
        <a class="navbar-brand" href="/"><asset:image src="grace.svg" alt="Grace Logo"/></a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarContent" aria-controls="navbarContent" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" aria-expanded="false" id="navbarContent">
            <ul class="navbar-nav ms-auto navbar-nav-scroll" style="--bs-scroll-height: 100px;">
                <g:if env="development">
                <li class="nav-item dropdown">
                    <a href="#" class="nav-link dropdown-toggle" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Application Status</a>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" href="#">Environment: ${grails.util.Environment.current.name}</a></li>
                        <li><a class="dropdown-item" href="#">App profile: ${grailsApplication.config.grails?.profile}</a></li>
                        <li><a class="dropdown-item" href="#">App version:
                            <g:meta name="info.app.version"/></a>
                        </li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" href="#">Grace version:
                            <g:meta name="info.app.grailsVersion"/></a>
                        </li>
                        <li><a class="dropdown-item" href="#">Groovy version: ${GroovySystem.getVersion()}</a></li>
                        <li><a class="dropdown-item" href="#">JVM version: ${System.getProperty('java.version')}</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" href="#">Reloading active: ${grails.util.Environment.reloadingAgentEnabled}</a></li>
                    </ul>
                </li>
                <li class="nav-item dropdown">
                    <a href="#" class="nav-link dropdown-toggle" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Artefacts</a>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" href="#">Controllers: ${grailsApplication.controllerClasses.size()}</a></li>
                        <li><a class="dropdown-item" href="#">Domains: ${grailsApplication.domainClasses.size()}</a></li>
                        <li><a class="dropdown-item" href="#">Services: ${grailsApplication.serviceClasses.size()}</a></li>
                        <li><a class="dropdown-item" href="#">Tag Libraries: ${grailsApplication.tagLibClasses.size()}</a></li>
                    </ul>
                </li>
                <li class="nav-item dropdown">
                    <a href="#" class="nav-link dropdown-toggle" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Plugins</a>
                    <ul class="dropdown-menu">
                        <g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
                            <li><a class="dropdown-item" href="#">${plugin.name} - ${plugin.version}</a></li>
                        </g:each>
                    </ul>
                </li>
                </g:if>
                <li class="nav-item dropdown">
                    <a href="#" class="nav-link dropdown-toggle" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Management</a>
                    <ul class="dropdown-menu">
                        <g:each var="c" in="${grailsApplication.controllerClasses.sort { it.name } }">
                            <li class="controller">
                                <g:link class="dropdown-item" controller="${c.logicalPropertyName}">
                                    <g:message code="${c.name.uncapitalize()}.label" default="${c.name}"/>
                                </g:link>
                            </li>
                        </g:each>
                    </ul>
                </li>
                <li class="nav-item dropdown">
                    <a href="#" class="nav-link dropdown-toggle" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Languages</a>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <g:each var="lang" in="${['en', 'cs', 'da', 'de', 'es', 'fr', 'it', 'ja', 'nb', 'nl', 'pl', 'pt_BR', 'pt_PT', 'ru', 'sk', 'sv', 'th', 'zh_CN', 'zh_TW']}">
                            <g:set var="locale" value="${Locale.forLanguageTag(lang.replace('_', '-'))}"/>
                            <g:set var="paramsWithLang" value="${params + [lang:lang]}"/>
                            <li>
                                <g:link class="dropdown-item" action="${actionName}" params="${paramsWithLang}">
                                    ${locale.getDisplayName(locale)} - (${lang})
                                </g:link>
                            </li>
                        </g:each>
                    </ul>
                </li>
                <li class="nav-item dropdown">
                    <a id="bootswatch-navlink" href="#" class="nav-link dropdown-toggle" data-bs-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Themes</a>
                    <ul id="bootswatch-themes" class="dropdown-menu dropdown-menu-end">
                        <li><a class="dropdown-item" href="#" onclick="changeTheme('default', '');return false;">Default</a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container">
    <div class="main-header">
        <div class="main-page-header">
            <h3 class="p-4"><g:message code="admin.console.title" default="Admin Console"/> </h3>
        </div>
        <admin:tabs css="" currentcss="active" role="tablist">
            <a class="nav-link [class]" href="[url]" title="[description]">[name]</a>
        </admin:tabs>
    </div>

    <div id="ui-main" class="row">
        <g:if test="${pageProperty(name:'meta.admin.active.item') != 'admin_summary'}">
            <div class="sidebar-container col-md-2">
                <div class="sidebar">
                    <admin:sidebar css="" currentcss="active" headercss="category">
                        <a href="[url]" class="nav-link [class]" title="[description]">[name]</a>
                    </admin:sidebar>
                </div>
            </div>
            <div id="ui-content" class="col-md-10">
                <g:layoutBody/>
            </div>
        </g:if>
        <g:if test="${pageProperty(name:'meta.admin.active.item') == 'admin_summary'}">
            <div id="ui-content" class="col-md-12">
                <g:layoutBody/>
            </div>
        </g:if>
    </div>
</div>

<div class="footer" role="contentinfo">
    <div class="container-fluid">
        <div class="row">
            <div class="col-md-4 col-sm-12">
                <a href="https://github.com/grace-guides" target="_blank">
                    <asset:image src="guides.svg" alt="Grace Guides" class="float-left"/>
                </a>
                <strong class="centered"><a href="https://github.com/grace-guides" target="_blank">Grace Guides</a></strong>
                <p>Building your first Grace app? Looking to add security, or create a Single-Page-App? Check out the <a href="https://github.com/grace-guides" target="_blank">Grace Guides</a> for step-by-step tutorials.</p>

            </div>
            <div class="col-md-4 col-sm-12">
                <a href="http://docs.graceframework.org" target="_blank">
                    <asset:image src="documentation.svg" alt="Grace Documentation" class="float-left"/>
                </a>
                <strong class="centered"><a href="http://docs.graceframework.org" target="_blank">Documentation</a></strong>
                <p>Ready to dig in? You can find in-depth documentation for all the features of Grace in the <a href="http://docs.graceframework.org" target="_blank">User Guide</a>.</p>

            </div>
            <div class="col-md-4 col-sm-12">
                <a href="https://github.com/orgs/grace-community/discussions" target="_blank">
                    <asset:image src="discussion.svg" alt="Grace Community" class="float-left"/>
                </a>
                <strong class="centered"><a href="https://github.com/orgs/grace-community/discussions" target="_blank">Join the Community</a></strong>
                <p>Get feedback and share your experience with other Grace developers in the <a href="https://github.com/orgs/grace-community/discussions" target="_blank">Grace Community</a>.</p>
            </div>
        </div>
    </div>
</div>

<div id="spinner" class="spinner" style="display:none;">
    <div class="d-flex justify-content-center">
        <div class="spinner-border text-primary" role="status">
            <span class="sr-only">Loading...</span>
        </div>
    </div>
</div>

<asset:javascript src="jquery.js"/>
<asset:javascript src="bootstrap.bundle.js"/>
<asset:javascript src="@grace/admin.js"/>

<g:if test='${asset.assetPathExists(src: "${controllerName}.js")}'>
    <asset:javascript src="${controllerName}.js"/>
</g:if>
<g:if test='${asset.assetPathExists(src: "${controllerName}-${actionName}.js")}'>
    <asset:javascript src="${controllerName}-${actionName}.js"/>
</g:if>

<g:pageProperty name="page.scripts"/>
</body>
</html>
