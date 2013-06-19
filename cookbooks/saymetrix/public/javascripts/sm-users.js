/* -------- DATATABLES -------- */
/* Table initialisation */
$(document).ready(function() {
    $('#users').dataTable({
        "sDom" : "<'row-fluid'<'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        "sPaginationType" : "bootstrap",
        "oLanguage" : {
            "sLengthMenu" : "_MENU_ records per page",
            "sSearch" : "",
            "sEmptyTable": "There are no Users in the database"
        },
        "bServerSide" : true,
        "sAjaxSource" : rootFolder+'/api/users.json',
        "aoColumns" : [{
            "mDataProp" : null,
            "sWidth" : '8%',
            "fnRender" : function(oObj) {
                return oObj.aData.firstname + ' ' + oObj.aData.lastname;
            }
        }, {
            "mDataProp" : 'email',
            "sWidth" : '10%'
        }, {
            "mDataProp" : 'role',
            "sWidth" : '5%',
            "fnRender" : function(oObj) {
                return oObj.aData.role.longname;
            }
        },  {
            "mDataProp" : null,
            "bSearchable" : false,
            "bSortable" : false,
            "sClass" : 'actions',
            "sWidth" : '4.55%',
            "fnRender" : function(oObj) {
                return "<div class='acct-btns'><a class='btn btn-mini acct-edit' data-id='" + oObj.aData['email'] + "' href='#edit'>Edit</a><a class='btn btn-mini btn-danger acct-del' data-toggle='modal' data-backdrop='static' data-id='" + oObj.aData['email'] + "' href='#delete'>Delete</a></div>";
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
    $('#users_wrapper > div:first-child').append('<div class="span6"><a class="btn btn-success acct-create" data-backdrop="static" href="#create">Create</a></div>');

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
        $(this).deleteModal('webuser', rootFolder+'/users');
    }));

    //When the delete modal is removed from view, just quickly update the table
    $('#delete').on('hidden', function() {
        $('#users').dataTable().fnDraw(false);
    });
	
    /* -------- CREATE MODAL -------- */
    $('.acct-create').click(function(){
        $('body').data('ajaxType', 'post');
        $('#create form').attr('action', rootFolder+'/users');
        $('#create .modal-header h3').text('Create User');
        $('#createBtn').text('Create');
        $('#create').modal('show');
    });
    
    //Get a list of Roles for the create modal
    $.ajax({
        type:"GET",
        url: rootFolder+"/api/users/roles.json"
    }).done(function(e){
        var roles = e;
        $.each(roles, function(key, value){
            $('#user-roles').append('<option value="'+value.name+'">'+value.longname+'</option>');
        })
        $('#user-roles').select2({
            placeholder : "Select a Role"
        });
    });

    //Declare method for when create button in create modal fails
    var userFailFn = function(obj, err, msg) {
        
        var response = $.parseJSON(obj.responseText);
        if (response["user.email"]) {
            $('input[name="user.email"]').closest('.control-group').addClass("error");
            $('input[name="user.email"]').siblings('.help-inline').text(response["user.email"]);
        }
        if (response["user.firstname"]) {
            $('input[name="user.firstname"]').closest('.control-group').addClass("error");
            $('input[name="user.firstname"]').siblings('.help-inline').text(response["user.firstname"]);
        }
        if (response["user.lastname"]) {
            $('input[name="user.lastname"]').closest('.control-group').addClass("error");
            $('input[name="user.lastname"]').siblings('.help-inline').text(response["user.lastname"]);
        }
        if (response["user.password"]) {
            $('input[name="user.password"]').closest('.control-group').addClass("error");
            $('input[name="user.password"]').siblings('.help-inline').text(response["user.password"]);
        }
        if (response["user.role"]) {
            $('select[name="user.role.name"]').closest('.control-group').addClass("error");
            $('select[name="user.role.name"]').siblings('.help-inline').text(response["user.role"]);
        }
    }
	
    var userDoneFn = function() {
        $("#alertBox span").text($('#create input[name="user.email"]').val());
        $('#users').dataTable().fnDraw(false);
    }
	
    //Add handler for create button inside create modal
    $('#createBtn').click(function(e){
        var newVal = $('#create input[name="user.email"]').val();
        $('#create input[name="user.email"]').val(newVal);
        $().submitCreateModal(e, 'webuser', '#createBtn', '#create', userDoneFn, userFailFn);
    });

    //When modal is removed from view...
    $('#create').on('hidden', function() {
        $().clearModalErrors();
        $('.modal-body input').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
        $('.modal-body input.placeholder-Password').val("Password");
        $('#user-roles').select2("val", "");
    });

    /* -------- EDIT MODAL -------- */
    //Add handler for Edit buttons
    $(document).on('click', '.acct-edit', (function() {
        var aKey = $(this).data('id');
        $.ajax({
            type : "GET",
            url : rootFolder+'/users/'+aKey
        }).done(function(obj) {
            $('#create input[name="user.email"]').val(obj.email).trigger('blur.placeholder');
            $('#create input[name="user.firstname"]').val(obj.firstname).trigger('blur.placeholder');
            $('#create input[name="user.lastname"]').val(obj.lastname).trigger('blur.placeholder');
            $('#create input[name="user.password"]').val(obj.password).trigger('blur.placeholder');
            $('#user-roles').select2('val',(obj.role.name));
            $('#create .modal-header h3').text('Edit User');
            $('body').data('ajaxType', 'put');
            $('#create form').attr('action', rootFolder+'/users/'+aKey);
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
