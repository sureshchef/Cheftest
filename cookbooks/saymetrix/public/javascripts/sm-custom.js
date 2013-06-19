/*
 * Used as the on click handler for delete buttons in tables.
 * type: String used to indicate the type of object being deleted. Used in the header and body.
 * url: String used for the ajax request, should start with a /.
 * 
 * Eg: $(document).on('click', '.acct-del', (function() {
 *       $(this).deleteModal('object', '/url/to/delete');
 *     }));
 */
jQuery.fn.deleteModal = function(type, url) {
	var aKey = $(this).data('id');
    $('#delete .modal-body span').text(aKey);
    $('#delete .modal-footer .btn-danger').click(function(e) {
    	e.preventDefault();
        $().toggleModalGUI('#delete .modal-footer .btn-danger', '#delete', true);
        $.ajax({
        	type: 'DELETE',
        	url: url+'/' + aKey
        }).always(function() {
        	$().toggleModalGUI('#delete .modal-footer .btn-danger', '#delete', false);
        	$('#delete').modal('hide');
        }).done(function() {
    		displayAlertBox(type + " Deleted", "The " + type + " "+ aKey + " was successfully deleted.");
        }).fail(function(obj, err, msg) {
        	var errMsg = "The " + type + " "+ aKey + " was not deleted.";
    		var response = $.parseJSON(obj.responseText);
            if (response.message) {
    			errMsg = response.message;
                        var errorCode = response.code;
                        var heading = response.heading;
                        $(this).displayErrorDialog(heading, errMsg, errorCode );
    		}else {
                    $(this).displayErrorDialog(errMsg);
                }
        });
    });
    $('#delete').modal('show');
};

/*
 * Used as the on click handler for submit button inside a modal.
 * e: The event that gets passed by the .click() method.
 * objectType: The type of object expected back (eg: account).
 * btn: String containing the ID of the submit button.
 * modal: String containing the ID of the modal.
 * ajaxType: The type of ajax request to make (i.e. post/put/get).
 * doneFn: The method to call if the request is positive. Will be called after this method does its own done() logic.
 * failFn: The method to call if the a 400 response is sent back by the server. Will be called before this method does any of its fail() logic.
 * 
 * Eg: $('#createBtn').click(function(e){
 *       $().submitCreateModal(e, 'account', '#createBtn', '#create', accountDoneFn, accountFailFn);
 *     });
 */ 
jQuery.fn.submitCreateModal = function(e, objectType, btn, modal, doneFn, failFn) {
	e.preventDefault();
	$().clearModalErrors();
    $().toggleModalGUI(btn, modal, true);
    var ajaxType = $('body').data('ajaxType');
	$.ajax({
		type : ajaxType,
		url : $(modal+' form').attr('action'),
		data : $(modal+' form').serialize(),
		dataType : 'json'
	}).always(function() {
		$().toggleModalGUI(btn, modal, false);
		$(btn).click(function(e){
			$().submitCreateModal(e, objectType, btn, modal, doneFn, failFn);
		});
	}).done(function() {
		$(modal).modal('hide');
		if (ajaxType.toUpperCase() == "POST") {
			displayAlertBox(objectType + " Added", "The " + objectType + " <span></span> was successfully added.");
		} else if (ajaxType.toUpperCase() == "PUT") {
			displayAlertBox(objectType + " Updated", "The " + objectType + " <span></span> was successfully updated.");
		}
		doneFn();
	}).fail(function(obj, err, msg) {
		console.log(obj);
		console.log(err);
		console.log(msg);
		failFn(obj, err, msg);
	});
}

/*
 * Used to disable buttons and activate spinners or modal views.
 * 
 * Eg: jQuery.fn.someModalThing = function(...) {
 *       ...
 *       $().toggleModalGUI('#btnId', '#modalId', true);
 *       ...
 *     });
 */
jQuery.fn.toggleModalGUI = function(btn, modal, disable) {
	if (disable) {
		$(btn).off("click");
		$(btn).button('toggle');
		$(modal+' .modal-footer a:first').attr('disabled', 'true');
		$(modal).data('modal').isShown = false;
		$(modal+' .modal-header .spinner').show();
	} else {
		$(modal+' .modal-header .spinner').hide();
		$(btn).button('toggle');
		$(modal+' .modal-footer a:first').removeAttr('disabled');
		$(modal).data('modal').isShown = true;
	}
}

/*
 * Used to clear error messages for a form inside a modal.
 * 
 * Eg: $('#create').on('hidden', function() {
 *       $().clearModalErrors();
 *       $('.modal-body input').val("");
 *     });
 */ 
jQuery.fn.clearModalErrors = function() {
	$('.modal-body .help-inline').text("");
	$('.modal-body').find('.error').removeClass('error');
}

/*
 * Used to display a save alert message briefly.
 * 
 * Eg: setTimeout(function(){displayAlertBox("title", "body")}, 500);
 */ 
var displayAlertBox = function(title, body) {
	$('#alertBox .title').html(title);
	$('#alertBox .body').html(body);
	$("#alertBox").fadeIn(150);
	$("#alertBox").click(function () {
		$("#alertBox").fadeOut(150);
	});
    $("#alertBox").delay(2000).fadeOut(150);
}

jQuery.fn.displayErrorDialog = function(heading, message, errorCode) {
    $('#errorDialog .title').html(heading);
	$('#errorDialog .body').html(message);
    $('#errorDialog .error-code').html((errorCode ? "If the problem persists please contact support and supply the error code: "+errorCode:""));
    $('#errorDialog').modal('show');
}


/* DataTables plugin for delaying the search filter */
jQuery.fn.dataTableExt.oApi.fnSetFilteringDelay = function ( oSettings, iDelay ) {
    var _that = this;
 
    if ( iDelay === undefined ) {
        iDelay = 250;
    }
      
    this.each( function ( i ) {
        $.fn.dataTableExt.iApiIndex = i;
        var
            $this = this,
            oTimerId = null,
            sPreviousSearch = null,
            anControl = $( 'input', _that.fnSettings().aanFeatures.f );
          
            anControl.unbind( 'keyup' ).bind( 'keyup', function() {
            var $$this = $this;
  
            if (sPreviousSearch === null || sPreviousSearch != anControl.val()) {
                window.clearTimeout(oTimerId);
                sPreviousSearch = anControl.val(); 
                oTimerId = window.setTimeout(function() {
                    $.fn.dataTableExt.iApiIndex = i;
                    _that.fnFilter( anControl.val() );
                }, iDelay);
            }
        });
          
        return this;
    } );
    return this;
};