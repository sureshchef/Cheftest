/* -------- DATATABLES -------- */
/* Table initialisation */
$(document).ready(function() {
    $('#events').dataTable({
        "sDom" : "<'row-fluid'<'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        "sPaginationType" : "bootstrap",
        "oLanguage" : {
            "sLengthMenu" : "_MENU_ records per page",
            "sSearch" : "",
            "sEmptyTable": "No events have been recorded."
        },
        "bServerSide" : true,
        "sAjaxSource" : rootFolder+'/api/events.dt',
        "aoColumns" : [{
            "mDataProp" : 'timestamp',
            "sWidth" : '18%',
            "bSortable" : false
        }, {
            "mDataProp" : 'type',
            "sWidth" : '20%',
            "bSortable" : false
        }, {
            "mDataProp" : 'actor',
            "sWidth" : '15%',
            "bSortable" : false
        },{
            "mDataProp" : 'details',
            "bSortable" : false
        }],
        "fnPreDrawCallback" : function() {
            $('.spinner').show();
        },
        "fnDrawCallback" : function() {
            $('.spinner').hide();
            var totalEvents = $('#events').dataTable().fnSettings().fnRecordsTotal();
            if(Math.ceil((this.fnSettings().fnRecordsDisplay()) / this.fnSettings()._iDisplayLength) > 1 ) {
                $('.dataTables_paginate').css("display", "block");
            } else {
                $('.dataTables_paginate').css("display", "none");
            }
        }
    }).fnSetFilteringDelay();

    $(".dataTables_filter > label").replaceWith(function() {
        return $('input', this);
    });
    $('.dataTables_filter').addClass('input-prepend');
    $('.dataTables_filter input').before('<span class="add-on"><i class="icon-search"></i></span>');
    $('.dataTables_filter input').attr('id', 'inputIcon');
    /* Add placeholder text to search field */
    $(".dataTables_filter input").attr('placeholder', 'Filter');
    $(".dataTables_filter input").after('<img class="spinner" src="'+rootFolder+'/public/images/spinner.gif">');
});
