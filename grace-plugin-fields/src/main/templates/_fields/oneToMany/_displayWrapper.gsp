<g:set var="controllerName" value="${persistentProperty.associatedEntity.decapitalizedName}"/>
<g:set var="shortName" value="${persistentProperty.associatedEntity.javaClass.simpleName}"/>

<ul>
<g:each var="item" in="${value}">
    <li><g:link controller="${controllerName}" action="show" id="${item.id}">${item}</g:link></li>
</g:each>
</ul>