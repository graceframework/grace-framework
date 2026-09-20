$(document).ajaxStart(function() {
    $("#spinner").show();
});
$(document).ajaxStop(function() {
    $("#spinner").hide();
});
