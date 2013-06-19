/* -------- DATATABLES -------- */
/* Table initialisation */
$(document).ready(function() {
    $('#subscribers').dataTable({
        "sDom" : "<'row-fluid'<'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        "sPaginationType" : "bootstrap",
        "oLanguage" : {
            "sLengthMenu" : "_MENU_ records per page",
            "sSearch" : "",
            "sEmptyTable": "There are no Subcribers in the database"
        },
        "bServerSide" : true,
        "sAjaxSource" : rootFolder+'/api/subscribers.json',
        "aoColumns" : [{
            "mDataProp" : null,
            "sWidth" : '13%',
            "fnRender" : function(oObj) {
                return oObj.aData.firstname + ' ' + oObj.aData.lastname;
            }
        }, {
            "mDataProp" : 'email',
            "sWidth" : '10%'
        }, {
            "mDataProp" : 'msisdn',
            "sWidth" : '10%'
        },{
            "mDataProp" : null,
            "sWidth" : '10%',
            "fnRender" : function(oObj) {
                if (oObj.aData.account != undefined){
                    return oObj.aData.account.key;
                }else {
                    return "";
                }
            }
        }, {
            "mDataProp" : null,
            "bSearchable" : false,
            "bSortable" : false,    
            "sClass" : 'actions',
            "sWidth" : '15%',
            "fnRender" : function(oObj) {
                return "<div class='acct-btns'><a class='btn btn-mini acct-edit' data-id='" + oObj.aData['msisdn'] + "' href='#edit'>Edit</a><a class='btn btn-mini btn-danger acct-del' data-toggle='modal' data-backdrop='static' data-id='" + oObj.aData['msisdn'] + "' href='#delete'>Delete</a></div>";
            }
        }],
        "fnPreDrawCallback" : function() {
            $('.spinner').show();
        },
        "fnDrawCallback" : function() {
            $('.spinner').hide();
            if(Math.ceil((this.fnSettings().fnRecordsDisplay()) / this.fnSettings()._iDisplayLength) > 1 ) {
                $('.dataTables_paginate').css("display", "block");
            } else {
                $('.dataTables_paginate').css("display", "none");
            }
        }
    }).fnSetFilteringDelay();
    /* Add the Create button */
    $('#subscribers_wrapper > div:first-child').append('<div class="span6"><a class="btn btn-success acct-create" data-backdrop="static" href="#create">Create</a></div>');

    /*  */
    $(".dataTables_filter > label").replaceWith(function() {
        return $('input', this);
    });
    $('.dataTables_filter').addClass('input-prepend');
    $('.dataTables_filter input').before('<span class="add-on"><i class="icon-search"></i></span>');
    $('.dataTables_filter input').attr('id', 'inputIcon');
    /* Add placeholder text to search field */
    $(".dataTables_filter input").attr('placeholder', 'Filter');
    $(".dataTables_filter input").after('<img class="spinner" src="'+rootFolder+'/public/images/spinner.gif">');

    /* -------- DELETE MODAL -------- */
    //Add handler for delete buttons
    $(document).on('click', '.acct-del', (function() {
        $(this).deleteModal('subscriber', rootFolder+'/subscribers');
    }));

    //When the delete modal is removed from view, just quickly update the table
    $('#delete').on('hidden', function() {
        $('#subscribers').dataTable().fnDraw(false);
    });

    /* -------- CREATE MODAL -------- */
    $('.acct-create').click(function(){
        $('body').data('ajaxType', 'post');
        $('#create form').attr('action', rootFolder+'/subscribers');
        $('#create .modal-header h3').text('Create Subscriber');
        $('#createBtn').text('Create');
        $('#create').modal('show');
    });
      
    //Get a list of accounts for the create modal
    $.ajax({
        type:"GET",
        url: rootFolder+"/api/accounts/accounts.json"
    }).done(function(e){
        var accounts = e;
        $.each(accounts, function(key, value){
            $('#accounts').append('<option value="'+value.key+'">'+value.name+'</option>');
        })
        $('#accounts').select2({
            placeholder : "Select an Account",
            allowClear : true
        });
    });
    //Declare method for when create button in create modal fails
    var subscriberFailFn = function(obj, err, msg) {
        var response = $.parseJSON(obj.responseText);
        if (response["s.email"]) {
            $('input[name="email"]').closest('.control-group').addClass("error");
            $('input[name="email"]').siblings('.help-inline').text(response["s.email"]);
        }
        if (response["s.firstname"]) {
            $('input[name="firstname"]').closest('.control-group').addClass("error");
            $('input[name="firstname"]').siblings('.help-inline').text(response["s.firstname"]);
        }
        if (response["s.lastname"]) {
            $('input[name="lastname"]').closest('.control-group').addClass("error");
            $('input[name="lastname"]').siblings('.help-inline').text(response["s.lastname"]);
        }
        if (response["s.msisdn"]) {
            $('input[name="msisdn"]').closest('.control-group').addClass("error");
            $('input[name="msisdn"]').siblings('.help-inline').text(response["s.msisdn"]);
        }
        if (response["s.account"]) {
            $('select[name="account"]').closest('.control-group').addClass("error");
            $('select[name="account"]').siblings('.help-inline').text(response["s.account"]);
        }
        
    }
	
    var subscriberDoneFn = function() {
        $("#alertBox span").text($('#create input[name="firstname"]').val());
        $('#subscribers').dataTable().fnDraw(false);
    }
	
    //Add handler for create button inside create modal
    $('#createBtn').click(function(e){
        var newVal = $('#create input[name="email"]').val();
        $('#create input[name="email"]').val(newVal);
        $().submitCreateModal(e, 'subscriber', '#createBtn', '#create', subscriberDoneFn, subscriberFailFn);
        $('#accounts').select2("val", "");
    });

    //When modal is removed from view...
    $('#create').on('hidden', function() {
        $().clearModalErrors();
        $('.modal-body input').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
    });

    /* -------- EDIT MODAL -------- */
    //Add handler for Edit buttons
    $(document).on('click', '.acct-edit', (function() {
        var aKey = $(this).data('id');
        $.ajax({
            type : "GET",
            url : rootFolder+'/subscribers/'+aKey
        }).done(function(obj) {
            $('#create input[name="email"]').val(obj.email).trigger('blur.placeholder');
            $('#create input[name="firstname"]').val(obj.firstname).trigger('blur.placeholder');
            $('#create input[name="lastname"]').val(obj.lastname).trigger('blur.placeholder');
            $('#create input[name="msisdn"]').val(obj.msisdn).trigger('blur.placeholder');
            $('#accounts').select2('val',obj.account.key).trigger('blur.placeholder');
            $('#create .modal-header h3').text('Edit Subscriber');
            $('body').data('ajaxType', 'put');
            $('#create form').attr('action', rootFolder+'/subscribers/'+aKey);
            $('#createBtn').text('Update');
            $('#create').modal('show');
        }).fail(function(obj, err, msg) {
            //error - can't find the original account object.
            console.log(obj);
            console.log(err);
            console.log(msg);
        });
    }));
});
