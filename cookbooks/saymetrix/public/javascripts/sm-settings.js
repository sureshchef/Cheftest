$('#forgot-password').click(function(e) {
    e.preventDefault();
    $.ajax({
        type:"GET",
        url: rootFolder+"/settings/password/forgot"
    }).done(function(obj){
        displayAlertBox("Reminder Sent.", "A password reset request has been sent to your email.");
    }).fail(function(obj, err, msg){
        console.log(obj, err, msg);
    });
});

$('#save-password').click(function(e) {
    e.preventDefault();
    clearErrors();
    $.ajax({
        type:"POST",
        url: rootFolder+"/settings/password/change",
        data: $('#password-form').serialize(),
        dataType: "json"
    }).done(function(obj) {
        displayAlertBox("Success.", "Your password has been changed.");
        clearErrors();
        $('#password-form input').val("");
    }).fail(function(obj, err, msg) {
        var response = $.parseJSON(obj.responseText);
        addErrorToInput('#current-password', response["current-password"]);
        addErrorToInput('#new-password', response["new-password"]);
    });
});


function displayAlertBox(title, body) {
	$('#alertBox .title').html(title);
	$('#alertBox .body').html(body);
	$("#alertBox").fadeIn(150);
	$("#alertBox").click(function () {
		$("#alertBox").fadeOut(150);
	});
    $("#alertBox").delay(2000).fadeOut(150);
}

function addErrorToInput(identifier, error) {
    if (error) {
        $(identifier).addClass("error");
        $(identifier+' .help-inline').html(error);
    }
}

function clearErrors() {
    $('.help-inline').html("");
    $('.error').removeClass('error');
}