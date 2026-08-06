<g:if test="${type in [Boolean, null]}">
    <g:formatBoolean boolean="${value}"/>
</g:if>
<g:elseif test="${type in [Calendar, Date, java.sql.Date, java.sql.Time]}">
    <g:formatDate date="${value}" format="yyyy-MM-dd HH:mm"/>
</g:elseif>
<g:else>
    <g:fieldValue bean="${bean}" field="${property}"/>
</g:else>