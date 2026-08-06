<g:if test="${constraints?.inList}">
<g:select name="${property}" from="${constraints.inList}" value="${value}" class="form-control custom-select col-2"/>
</g:if>
<g:else>
<g:select name="${property}" from="${type.values()}" value="${value}" class="form-control custom-select col-2"/>
</g:else>
