//Create Network Event Stuff
var eventMap, eventMarkers, editingEvent = false;
function initialiseEventCreateMap(googleMapOptions) {
    eventMap = new google.maps.Map(document.getElementById("event-map"), googleMapOptions);
    $.ajax({
        type:"GET",
        url: rootFolder+"/api/sites.json"
    }).done(function(sites){
        eventMarkers = [];
        $.each(sites, function(index,site){
            $('#event-sites').append('<option value="'+site.key+'">'+site.key+'</option>');
            //and also create marker on map
            //create marker
            var eventMarker = new google.maps.Marker({
                position:new google.maps.LatLng(site.latitude, site.longitude),
                map: eventMap,
                draggable:false,
                title:site.key,
                select2: false,
                icon: rootFolder+"/public/images/map-icons/markers/measle_grey.png"
            });
            google.maps.event.addListener(eventMarker, 'click', function(e) {
                var oldValues = $('#event-sites').select2("val");
                if (this.select2) {
                    unselectSite(this);
                    oldValues.remove(this.getTitle());
                    $('#event-sites').select2("val", oldValues);
                } else {
                    selectSite(this);
                    oldValues.push(this.getTitle());
                    $('#event-sites').select2("val", oldValues);
                }
            });
            $('#event-sites').on("change", function(e) {
                var selMarker;
                if (e.removed) {
                    selMarker = eventMarkers[e.removed.id];
                    unselectSite(selMarker);
                } else {
                    selMarker = eventMarkers[e.added.id];
                    selectSite(selMarker);
                    eventMap.setCenter(selMarker.getPosition());
                }
            });
            eventMarkers[site.key] = eventMarker;
        });
        $('#event-sites').select2({
            placeholder : "Select a Site",
            allowClear : true
        });
    });
}

function selectSite(marker) {
    marker.setIcon(rootFolder+"/public/images/map-icons/markers/measle_blue.png");
    marker.select2 = true;
}

function unselectSite(marker) {
    marker.setIcon(rootFolder+"/public/images/map-icons/markers/measle_grey.png");
    marker.select2 = false;
}

function getReadableDate(raw) {
    var monthNames = [ "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" ];

    var d = new Date(raw);
    return d.getDate() + " " + monthNames[d.getMonth()] + ", " + d.getFullYear();
}

function openEventModal() {
    $('#create-event').modal('show');
    $('#create-event .spinner').hide();
    var date = new Date();
    $('#event-start-date').datepicker('setValue', date);  //need to set date here so that its filled when user comes to it
    $('#event-start-time').timepicker('setTime', date.toTimeString());  //need to set time here so its the most up to date
    $('#event-end-date').datepicker('setValue', date);
    $('#event-end-time').timepicker('setTime', date.toTimeString());
    showEventDetailsTab();
    resetEventMap();
}

function showEventDetailsTab() {
    $('#event-tabs a[href="#event-details"]').tab('show');
    $('#event-back').addClass("disabled");
    $('#event-create').text("Next");
    $('#create-event .modal-header h3 small').text('- Event Details');
}

function showEventSitesTab() {
    $('#event-tabs a[href="#event-location"]').tab('show');
    $('#event-back').removeClass("disabled");
    if (editingEvent) {
        $('#event-create').text("Update");
    } else {
        $('#event-create').text("Create");
    }
    $('#create-event .modal-header h3 small').text('- Event Sites');
    refreshEventMap();
}

function resetEventMap() {
    for (key in eventMarkers) {
        if (typeof eventMarkers[key] != 'function') {
            unselectSite(eventMarkers[key]);
        }
    }
    eventMap.setCenter(centerLatLng);
    eventMap.setZoom(10);
}

function refreshEventMap() {
    google.maps.event.trigger(eventMap, 'resize');
}

function fitMapToMarkers() {
    var bounds = new google.maps.LatLngBounds();
    for (key in eventMarkers) {
        if (typeof eventMarkers[key] != 'function' && eventMarkers[key].select2) {
            bounds.extend(eventMarkers[key].getPosition());
        }
    }
    eventMap.fitBounds(bounds);
}

function checkDateValid(selector) {
    var utcDateString = null;
    try {
        var selectedDate = $(selector+'-date input').val();
        if (selectedDate == "") {
            addErrorToInput(selector+'-date', "Required");
        } else {
            //hack for getting time/date string into UTC and in format that Server will like
            utcDateString = new Date(selectedDate.substring(3, 6) + selectedDate.substring(0, 3) + selectedDate.substring(6) + " " + $(selector+'-time').val()).toISOString();
        }
    } catch (e) {
        addErrorToInput(selector+'-date', "Please enter a date in the format: dd/mm/yyyy");
    }
    return utcDateString;
}

function postEventData() {
    toggleUI('event', false);
    var startUTC = checkDateValid('#event-start');
    var endUTC = checkDateValid('#event-end');
    //get the data and ajax it
    var formData = {
        "start-date": startUTC,
        "end-date": endUTC,
        "event-type": $('#event-types').select2("val"),
        "subject": $('#create-event input[name=event-subject]').val(),
        "description": $('#create-event textarea').val(),
        "sites": $('#event-sites').select2("val")
    };
    var ajaxType = 'POST';
    var urlAddOn = '';
    if (editingEvent) {
        ajaxType = 'PUT';
        urlAddOn = '/'+$('#create-event input[name=id]').val();
    }
    $.ajax({
        type : ajaxType,
        url : $('#event-location form').attr('action')+urlAddOn,
        data: JSON.stringify(formData),
        contentType: "application/json; charset=utf-8",
        dataType : 'json'
    }).always(function() {
        toggleUI('event', true);
    }).done(function(e) {
        $('#create-event').modal('hide');
        if (editingEvent) {
            displayAlertBox("Network Event Added", "The Network Event was successfully updated.");
        } else {
            displayAlertBox("Network Event Added", "The Network Event was successfully added.");
        }
        $('#networkevents').dataTable().fnDraw(false);
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
        var response = $.parseJSON(obj.responseText);
        if (obj.status == 400) {
            addErrorToInput('#event-start-date', response["ne.start-date"]);
            addErrorToInput('#event-end-date', response["ne.end-date"]);
            addErrorToInput('#create-event select[name="event-type"]', response["ne.eventType"]);
            addErrorToInput('#create-event input[name="event-subject"]', response["ne.subject"]);
            addErrorToInput('#create-event textarea[name="event-description"]', response["ne.description"]);
            addErrorToInput('#create-event select[name="event-sites"]', response["ne.sites"]);
            if (!response["ne.sites"] && (!response["ne.start-date"] || response["ne.end-date"] || response["ne.eventType"] || response["ne.subject"] || response["ne.description"])) {
                showEventDetailsTab();
            }
        }
    });
    
}

function setUpDataTables() {
    $('#networkevents').dataTable({
        "sDom" : "<'row-fluid'<'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        "sPaginationType" : "bootstrap",
        "oLanguage" : {
            "sLengthMenu" : "_MENU_ records per page",
            "sSearch" : "",
            "sEmptyTable": "There are no Network Events in the database"
        },
        "bServerSide" : true,
        "sAjaxSource" : rootFolder+'/api/events.json',
        "aoColumns" : [{
            "mDataProp" : 'subject',
            "sWidth" : '7%',
            "fnRender" : function(oObj) {
                return '<a href="' + rootFolder + '/events/' + + oObj.aData['id'] + '">' + oObj.aData.subject + '</a>';
            }
        }, {
            "mDataProp" : 'eventType',
            "sWidth" : '7%',
            "fnRender" : function(oObj) {
                return oObj.aData.eventType.name;
            }
        },  {
            "mDataProp" : 'eventPeriod',
            "bSearchable" : false,
            "bSortable" : false,
            "sWidth" : '9%',
            "fnRender" : function(oObj) {
                return getReadableDate(oObj.aData.eventPeriod.startMillis) + " - " + getReadableDate(oObj.aData.eventPeriod.endMillis);
            }
        }, {
            "mDataProp" : 'creator',
            "sWidth" : '6%',
            "fnRender" : function(oObj) {
                return oObj.aData.creator.firstname + " " + oObj.aData.creator.lastname;
            }
        }, {
            "mDataProp" : 'createdOn',
            "sWidth" : '5%',
            "fnRender" : function(oObj) {
                return getReadableDate(oObj.aData.createdOn.millis);
            }
        },  {
            "mDataProp" : null,
            "bSearchable" : false,
            "bSortable" : false,
            "sClass" : 'actions',
            "sWidth" : '6%',
            "fnRender" : function(oObj) {
                return "<div class='acct-btns'><a class='btn btn-mini acct-edit' data-id='" + oObj.aData['id'] + "' href='#edit'>Edit</a><a class='btn btn-mini btn-danger acct-del' data-toggle='modal' data-backdrop='static' data-id='" + oObj.aData['id'] + "' href='#delete'>Delete</a></div>";
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
    $('#networkevents_wrapper > div:first-child').append('<div class="span6"><a class="btn btn-success acct-create" data-backdrop="static" href="#create">Create</a></div>');

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
}

function initialiseModalItems() {
    //initialise everything else in the modal
    $('#event-start-date').datepicker();
    $('#event-start-time').timepicker({
        minuteStep: 1,
        showSeconds: false,
        showMeridian: false
    });
    $('#event-end-date').datepicker();
    $('#event-end-time').timepicker({
        minuteStep: 1,
        showSeconds: false,
        showMeridian: false
    });
    $('#event-types').select2();
}

function editEvent(id) {
    $.ajax({
        type : 'GET',
        url : rootFolder+'/api/events/'+id,
        dataType : 'json'
    }).done(function(obj) {
        openEventModal();
        $('#create-event .modal-header h3 span').text("Edit Network Event");
        editingEvent = true;
        //fill out inputs
        $('#event-details input[name=id]').val(id);
        $('#event-types').select2("val", obj.eventType.key);
        $('#create-event input[name=event-subject]').val(obj.subject);
        $('#create-event textarea').val(obj.description);
        var sites = [];
        for (var i = 0; i < obj.sites.length; i++) {
            sites.push(obj.sites[i].key);
            var selMarker = eventMarkers[obj.sites[i].key];
            selectSite(selMarker);
            eventMap.setCenter(selMarker.getPosition());
        }
        $('#event-sites').select2("val", sites);
        //date and time
        var iDate = new Date(obj.eventPeriod.startMillis);
        $('#event-start-date').datepicker('setValue', iDate);
        $('#event-start-time').timepicker('setTime', iDate.toTimeString());
        iDate = new Date(obj.eventPeriod.endMillis);
        $('#event-end-date').datepicker('setValue', iDate);
        $('#event-end-time').timepicker('setTime', iDate.toTimeString());
        //fitMapToMarkers();
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
    });
}

$(document).ready(function() {
    setUpDataTables();
    
    initialiseModalItems();
    
    //initial opening of modal
    $('.acct-create').click(function(){
        openEventModal();
    });

    $('#event-create').click(function(e) {
        if (!$('#event-create').hasClass('disabled')) {
            if ($('#event-details').hasClass("active")) {
                //check dates
                var moveon = true;
                var startUTC = checkDateValid('#event-start');
                var endUTC = checkDateValid('#event-end');
                if(startUTC && endUTC) {
                    if (fromISOString(startUTC) > fromISOString(endUTC)) {
                        moveon = false;
                        addErrorToInput('#event-start-date', "Please ensure the start date is before the end date");
                    }
                } else {
                    moveon = false;
                }
                if ($('#event-subject').val() == "") {
                    moveon = false;
                    addErrorToInput('#event-subject', "Required");
                }
                if ($('#create-event textarea[name="event-description"]').val() == "") {
                    moveon = false;
                    addErrorToInput('#create-event textarea[name="event-description"]', "Required");
                }
                if (moveon) {
                    showEventSitesTab();
                }
            } else if ($('#event-location').hasClass("active")) {
                postEventData();
            }
        }
    });

    //back button clicking
    $('#event-back').click(function(e) {
        if (!$('#event-back').hasClass('disabled')) {
            if ($('#event-location').hasClass("active")) {
                showEventDetailsTab();
            }
        }
    });

    //sort of a hack to get the tabs in the modal to render properly when re-opened in IE7
    $('#create-event').on('hide', function(e) {
        //need to check source, as the datepicker also fires this event
        if ($(e.target).hasClass("eventModal")) {
            showEventDetailsTab();
        } else if (e.target.id == "event-start-date") {
            //check date
            var startUTC = checkDateValid('#event-start');
            var endUTC = checkDateValid('#event-end');
            if(startUTC && endUTC) {
                if (fromISOString(startUTC) > fromISOString(endUTC)) {
                    $('#event-end-date').datepicker('setValue', new Date(fromISOString(startUTC)));
                }
            }
        }
    });

    //When modal is removed from view...
    $('#create-event').on('hidden', function() {
        $('#create-event .modal-body .help-inline').html("");
        $('#create-event .modal-body').find('.error').removeClass('error');
        $('#create-event .modal-body input').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
        $('#create-event .modal-body textarea').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
        $('#event-types').select2("val", "outage");
        $('#event-sites').select2("val", "");
        if (editingEvent) {
            $('#create-event .modal-header h3 span').text("Report Network Event");
            editingEvent = false;
        }
    });
    
    $('#event-site-all').click(function() {
        var newValues = [];
        for (key in eventMarkers) {
            if (typeof eventMarkers[key] != 'function') {
                selectSite(eventMarkers[key]);
                newValues.push(eventMarkers[key].getTitle());
            }
        }
        $('#event-sites').select2("val", newValues);
    });
    
    $('#event-site-none').click(function() {
        for (key in eventMarkers) {
            if (typeof eventMarkers[key] != 'function') {
                unselectSite(eventMarkers[key]);
            }
        }
        $('#event-sites').select2("val", []);
    });
    
    /* -------- DELETE MODAL -------- */
    //Add handler for delete buttons
    $(document).on('click', '.acct-del', (function() {
        var aKey = $(this).data('id');
        $('#delete .modal-footer .btn-danger').click(function(e) {
            e.preventDefault();
            $().toggleModalGUI('#delete .modal-footer .btn-danger', '#delete', true);
            $.ajax({
                type: 'DELETE',
                url: rootFolder+'/events/' + aKey
            }).always(function() {
                $().toggleModalGUI('#delete .modal-footer .btn-danger', '#delete', false);
                $('#delete').modal('hide');
            }).done(function() {
                displayAlertBox("Network Event Deleted", "The Network Event was successfully deleted.");
            }).fail(function(obj, err, msg) {
                var errMsg = "The Network Event was not deleted.";
                var response = $.parseJSON(obj.responseText);
                if (response.message) {
                    errMsg = response.message;
                    var errorCode = response.code;
                    var heading = response.heading;
                    $(this).displayErrorDialog(heading, errMsg, errorCode );
                } else {
                    $(this).displayErrorDialog(errMsg);
                }
            });
        });
        $('#delete').modal('show');
    }));

    //When the delete modal is removed from view, just quickly update the table
    $('#delete').on('hidden', function() {
        $('#networkevents').dataTable().fnDraw(false);
    });
	
    //Add handler for Edit buttons
    $(document).on('click', '.acct-edit', (function() {
        var aKey = $(this).data('id');
        editEvent(aKey);
    }));
});



var latitude = 53.29877372565976;
var longitude = -6.178543567657471;
var centerLatLng;
function initializeMaps() {
    centerLatLng = new google.maps.LatLng(latitude, longitude);
    var googleMapOptions = {
        center: centerLatLng,
        zoom: 12, //zoom level, 0 = earth view to higher value
        zoomControl: true, //enable zoom control
        zoomControlOptions: {
            style: google.maps.ZoomControlStyle.SMALL //zoom control size
        },
        streetViewControl: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP // google map type
    };
    
    initialiseEventCreateMap(googleMapOptions);
}
google.maps.event.addDomListener(window, 'load', initializeMaps);





Array.prototype.remove = function() {
    var what, a = arguments, L = a.length, ax;
    while (L && this.length) {
        what = a[--L];
        while ((ax = this.indexOf(what)) !== -1) {
            this.splice(ax, 1);
        }
    }
    return this;
};
if(!Array.prototype.indexOf) {
    Array.prototype.indexOf = function(what, i) {
        i = i || 0;
        var L = this.length;
        while (i < L) {
            if(this[i] === what) return i;
            ++i;
        }
        return -1;
    };
}