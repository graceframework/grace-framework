<g:if test="${type.isPrimitive() || type in [Integer, Number, java.math.BigInteger]}">
<g:field type="number" name="${property}" value="${value}" class="form-control col-2" min="${constraints.min}" max="${constraints.max}" step="1" />
</g:if>
<g:elseif test="${type in [Float, Double, java.math.BigDecimal]}">
<g:field type="number" name="${property}" value="${value}" class="form-control col-2" min="${constraints.min}" max="${constraints.max}" step="0.${'0' * (constraints.scale - 1)}1" />
</g:elseif>
<g:else>
<g:field type="number" name="${property}" value="${value}" class="form-control col-2"/>
</g:else>