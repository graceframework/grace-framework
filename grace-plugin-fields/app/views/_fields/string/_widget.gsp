<g:if test="${constraints?.inList}">
<g:select name="${property}" from="${constraints.inList}" value="${value}" class="form-control custom-select col-2"/>
</g:if>
<g:elseif test="${constraints?.widget == 'textarea'}">
<g:textArea name="${property}" value="${value}" rows="3" class="form-control" id="widget.${property}"/>
</g:elseif>
<g:else>
<g:textField name="${property}" value="${value}" required="${required}" class="form-control col-8" id="widget.${property}"/>
</g:else>
