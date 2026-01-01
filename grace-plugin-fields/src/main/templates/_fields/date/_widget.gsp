<div class="hstack gap-3">
<g:datePicker name="${property}" value="${value ?: new Date()}"
              precision="${attrs.precision}"
              cssClasses="[year: 'form-select w-20', 'month': 'form-select w-20', 'day': 'form-select w-20', 'hour': 'form-select w-20', 'minute': 'form-select w-20']"
              noSelection="['':'-Choose-']"/>
</div>