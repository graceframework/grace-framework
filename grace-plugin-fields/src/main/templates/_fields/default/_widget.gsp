<g:if test="${type in [Boolean, null]}">
<g:checkBox name="${property}" value="${value}" checked="${value ? 'true': 'false'}"/>
</g:if>
<g:else>
    ${type} ${bean}
</g:else>