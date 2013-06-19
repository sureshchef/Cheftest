/* -------- DATATABLES -------- */
/* Table initialisation */
$(document).ready(function() {
    $('#accounts').dataTable({
        "sDom" : "<'row-fluid'<'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        "sPaginationType" : "bootstrap",
        "oLanguage" : {
            "sLengthMenu" : "_MENU_ records per page",
            "sSearch" : "",
           "sEmptyTable": "There are no Accounts in the database"
        },
        "bServerSide" : true,
        "sAjaxSource" : rootFolder+'/api/accounts.dt',
        "aoColumns" : [{
            "mDataProp" : 'key',
            "sWidth" : '11.5%'
        }, {
            "mDataProp" : 'name',
            "sWidth" : '15%'
        }, {
            "mDataProp" : 'manager',
            "sWidth" : '15%',
            "fnRender" : function(oObj) {
                return oObj.aData.manager.firstname + ' ' + oObj.aData.manager.lastname;
            }
        }, {
            "mDataProp" : 'contact',
            "sWidth" : '15%',
            "sDefaultContent" : ''
        }, {
            "mDataProp" : null,
            "bSearchable" : false,
            "bSortable" : false,
            "sClass" : 'actions',
            "sWidth" : '13.5%',
            "fnRender" : function(oObj) {
                return "<div class='acct-btns'><a class='btn btn-mini acct-edit' data-id='" + oObj.aData['key'] + "' href='#edit'>Edit</a><a class='btn btn-mini btn-danger acct-del' data-toggle='modal' data-backdrop='static' data-id='" + oObj.aData['key'] + "' href='#delete'>Delete</a></div>";
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
    /* Add the Add Account button */
    $('#accounts_wrapper > div:first-child').append('<div class="span6"><a class="btn btn-success acct-create" data-backdrop="static" href="#create">Create</a></div>');

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
        $(this).deleteModal('account', rootFolder+'/api/accounts');
    }));

    //When the delete modal is removed from view, just quickly update the table
    $('#delete').on('hidden', function() {
        $('#accounts').dataTable().fnDraw(false);
    });
	    
    /* -------- CREATE MODAL -------- */
    $('.acct-create').click(function(){
        $('body').data('ajaxType', 'post');
        $('#create form').attr('action', rootFolder+'/accounts');
        $('#createBtn').text('Create');
        $('#create .modal-header span').text('Add');
        $('#create').modal('show');
    });
    //Get a list of managers for the create modal
    /* Ideally this would only happen when the modal gets created for the first time. */
    $.ajax({
        type:"GET",
        url: rootFolder+"/api/managers.json"
    }).done(function(managers){
        $.each(managers, function(index,user){
            $('#managers').append('<option value="'+user.id+'">'+user.firstname+" "+user.lastname+'</option>');
        })
        $('#managers').select2({
            placeholder : "Select a Manager",
            allowClear : true
        });
    });

    //Declare method for when create button in create modal fails
    var accountFailFn = function(obj, err, msg) {
        var response = $.parseJSON(obj.responseText);
        console.log("Creating account failed: "+response);
        if (response["account.key"]) {
            $('input[name="account.key"]').closest('.control-group').addClass("error");
            $('input[name="account.key"]').siblings('.help-inline').text(response["account.key"]);
        }
        if (response["account.name"]) {
            $('input[name="account.name"]').closest('.control-group').addClass("error");
            $('input[name="account.name"]').siblings('.help-inline').text(response["account.name"]);
        }
        if (response["account.contact"]) {
            $('input[name="account.contact"]').closest('.control-group').addClass("error");
            $('input[name="account.contact"]').siblings('.help-inline').text(response["account.contact"]);
        }
        if (response["account.manager"]) {
            $('select[name="account.manager.id"]').closest('.control-group').addClass("error");
            $('select[name="account.manager.id"]').siblings('.help-inline').text(response["account.manager"]);
        }
    }
	
    var accountDoneFn = function() {
        $("#alertBox span").text($('#create input[name="key"]').val());
        $('#accounts').dataTable().fnDraw(false);
    }
	
    //Add handler for create button inside create modal
    $('#createBtn').click(function(e){
        var accountKey = $('#create input[name="account.key"]').val().toUpperCase();
        $('#create input[name="account.key"]').val(accountKey);
        $().submitCreateModal(e, 'account', '#createBtn', '#create', accountDoneFn, accountFailFn);
    });

    //When modal is removed from view...
    $('#create').on('hidden', function() {
        $().clearModalErrors();
        $('.modal-body input').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
        $('#managers').select2("val", "");
    });

    /* -------- EDIT MODAL -------- */
    //Add handler for delete buttons
    $(document).on('click', '.acct-edit', (function() {
        var aKey = $(this).data('id');
        $.ajax({
            type : "GET",
            url : rootFolder+'/accounts/'+aKey
        }).done(function(obj) {
            $('#create input[name="account.key"]').val(obj.key);
            $('#create input[name="account.name"]').val(obj.name);
            $('#managers').select2('val', obj.manager.id);
            $('#create input[name="account.contact"]').val(obj.contact).trigger('blur.placeholder');
            $('body').data('ajaxType', 'put');
            $('#create form').attr('action', rootFolder+'/accounts/'+aKey);
            $('#createBtn').text('Update');
            $('#create .modal-header span').text('Update');
            $('#create').modal('show');
        }).fail(function(obj, err, msg) {
            //error - can't find the original account object.
            console.log(obj);
            console.log(err);
            console.log(msg);
        });
    }));
});
