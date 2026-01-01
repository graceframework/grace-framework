<form class="${domainClass.decapitalizedName}">
    <g:each in="${domainProperties}" var="p">
        <div class="form-group row">
            <label for="${p.name}" class="col-3 col-form-label property-label text-end"><g:message code="${domainClass.decapitalizedName}.${p.name}.label" default="${p.defaultLabel}" /></label>
            <div class="col-9">
                <div class="property-value" aria-labelledby="${p.name}">${body(p)}</div>
            </div>
        </div>
    </g:each>
</form>