<g:set var="controllerName" value="${persistentProperty.associatedEntity.decapitalizedName}"/>
<g:set var="shortName" value="${persistentProperty.associatedEntity.javaClass.simpleName}"/>
<g:each var="item" in="${persistentProperty.associatedEntity.javaClass.list()}">
<div class="custom-control-inline custom-control custom-checkbox">
  <g:checkBox class="custom-control-input" id="${item.id}" name="${property}" value="${item.id}" checked="${item.id in value*.id}" />
  <label class="custom-control-label" for="${item.id}">${item.name}</label>
</div>
</g:each>
<g:link controller="${controllerName}" action="create"><g:message code="default.create.label" args="[shortName]" default="${shortName}"/></g:link>
