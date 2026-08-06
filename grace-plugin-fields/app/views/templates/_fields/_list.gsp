<ol class="property-list ${domainClass.decapitalizedName}">
    <g:each in="${domainProperties}" var="p">
        <li class="form-group row">
            <label id="${p.name}-label" for="${p.name}" class="property-label col-3 col-form-label text-end"><g:message code="${domainClass.decapitalizedName}.${p.name}.label" default="${p.defaultLabel}" /></label>
            <div class="col-9">
                <div id="${p.name}" class="property-value" aria-labelledby="${p.name}-label">${body(p)}</div>
            </div>
        </li>
    </g:each>
</ol>