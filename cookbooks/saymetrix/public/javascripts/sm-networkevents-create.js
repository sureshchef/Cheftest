//Create Network Event Stuff
var eventMap, eventMarkers;
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

function openEventModal() {
    $('#create-event').modal('show');
    $('#create-event .spinner').hide();
    var date = new Date();
    $('#event-start-date').datepicker('setValue', date);  //need to set date here so that its filled when user comes to it
    $('#event-start-time').timepicker('setTime', date.toTimeString());  //need to set time here so its the most up to date
    $('#event-end-date').datepicker('setValue', date);
    $('#event-end-time').timepicker('setTime', date.toTimeString());
    showEventDetailsTab();
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
    $('#event-create').text("Create");
    $('#create-event .modal-header h3 small').text('- Event Sites');
    refreshEventMap();
}

function refreshEventMap() {
    google.maps.event.trigger(eventMap, 'resize');
}

function checkDateValid(selector) {
    var utcDateString = null;
    try {
        var selectedDate = $(selector+'-date input').val();
        if (selectedDate == "" || selectedDate == undefined) {
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
    $.ajax({
        type : 'POST',
        url : $('#event-location form').attr('action'),
        data: JSON.stringify(formData),
        contentType: "application/json; charset=utf-8",
        dataType : 'json'
    }).always(function() {
        toggleUI('event', true);
    }).done(function(e) {
        $('#create-event').modal('hide');
        Ext.MessageBox.alert('Success', 'Network Event successfully created.');
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
        } else if (obj.status == 500) {
            Ext.MessageBox.alert(response.heading, response.message);
        }
    });
    
}

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


//initial opening of modal
$('.event-create').click(function(e){
    e.preventDefault();
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






/*  Viewing NetworkEvent stuff - put here because it relates to creating events
 *******************************************************************************/

function openViewEventsModal() {
    $('#view-events').modal('show');
    $('#view-events .spinner').hide();
    var date = new Date();
    $('#view-events-date').datepicker('setValue', date);  //need to set date here so that its filled when user comes to it
    $('#view-events-time').timepicker('setTime', date.toTimeString());  //need to set time here so its the most up to date
    toggleViewEventsUI(false);
    toggleFilterUI(true);
    getAllEvents("date");
}

function toggleViewEventsUI(enable) {
    if (enable) {
        //re-enable/disable UI elements
        $('#view-events .modal-header .spinner').hide();
        $('#view-events-close').removeClass('disabled');
        $('#view-events .modal-footer button:first').removeAttr('disabled');
        $('#view-events').data('modal').isShown = true;
    } else {
        $('#view-events .modal-header .spinner').show();
        $('#view-events-close').addClass('disabled');
        $('#view-events .modal-footer button:first').attr('disabled', true);
        $('#view-events').data('modal').isShown = false;
    }
}

function clearEventsList() {
    $('#view-events-list').html("");
}

function getReadableDate(raw) {
    var monthNames = [ "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" ];

    var d = new Date(raw);
    return d.getDate() + " " + monthNames[d.getMonth()] + ", " + d.getFullYear() + " at " + d.getHours() + ":" + d.getMinutes();
}

function addEventToList(event) {
    var start = getReadableDate(event.eventPeriod.startMillis);
    var end = getReadableDate(event.eventPeriod.endMillis);
    var listHtml = '<div class="list-wrapper"><ol>';
    $.each(event.sites, function(i, site) {
        listHtml += '<li>'+site.key+'</li>';
    });
    listHtml += '</ol><br/></div>';
    var desc = event.description.replace(/\n/g, '<br />');
    var count = "+"+event.numOfIncidents;
    if (event.numOfIncidents < 1) {
        count = "";
    }
    $('#view-events-list').append('<div class="view-event" onclick="morelessclick(this);">'+
                '<div class="pull-right"><span style="margin-right:8px;vertical-align:middle;" title="'+event.numOfIncidents+' Incident(s) affected by this event">'+count+'</span><button class="btn btn-small" onclick="openIncidentPrepopulated('+event.id+')" title="My customer is affected by this event"><i class="icon-retweet"></i></button></div>'+
                '<h3>'+event.eventType.name+' <small>- '+event.subject+'</small></h3><h6>'+start+' - '+end+'</h6><div class="event-details"><p>'+desc+'</p><h6>Sites</h6>'+listHtml+'</div>'+
            '</div>');
}

function getEventsForDate(sort) {
    var dateUTC = checkDateValid('#view-events');
    
    $.ajax({
        type : 'GET',
        url : rootFolder+'/api/events-list/'+dateUTC+'?sort='+sort,
        dataType : 'json'
    }).done(function(events) {
        clearEventsList();
        if (events.length > 0) {
            $.each(events, function(index,event){
                addEventToList(event);
            });
            $('.event-details').shorten();
        } else {
            $('#view-events-list').append('<div class="events-list-holder"><h3>No Network Events occured during this date</h3><h4>'+$('#view-events-date input').val()+' at '+$('#view-events-time').val()+'</h4></div>');
        }
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
    }).always(function() {
        toggleViewEventsUI(true);
    });
}

function getAllEvents(sort) {
    $.ajax({
        type : 'GET',
        url : rootFolder+'/api/events-list/all?sort='+sort,
        dataType : 'json'
    }).done(function(events) {
        clearEventsList();
        if (events.length > 0) {
            $.each(events, function(index,event){
                addEventToList(event);
            });
            $('.event-details').shorten();
        } else {
            $('#view-events-list').append('<div class="events-list-holder"><h3>No Network Events have been recorded</h3></div>');
        }
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
    }).always(function() {
        toggleViewEventsUI(true);
    });
}

function openIncidentPrepopulated(id) {
    $('#view-events').modal('hide');
    editNetEventId = id; //this variable exists in sm-incidents.js
    var dateUTC = checkDateValid('#view-events');
    openNewIncident(new Date(fromISOString(dateUTC))); //this is a method in sm-incidents.js + trying not to do too much cross-scripting
}

function toggleFilterUI(disable) {
    if (disable) {
        $('#view-events-date input').addClass('disabled').attr("disabled", true);
        $('#view-events-date .btn').addClass('disabled').attr("disabled", true);
        $('#view-events .bootstrap-timepicker input').addClass('disabled').attr("disabled", true);
        $('#view-events .bootstrap-timepicker .btn').addClass('disabled').attr("disabled", true);
    } else {
        $('#view-events-date input').removeClass('disabled').removeAttr("disabled");
        $('#view-events-date .btn').removeClass('disabled').removeAttr("disabled");
        $('#view-events .bootstrap-timepicker input').removeClass('disabled').removeAttr("disabled");
        $('#view-events .bootstrap-timepicker .btn').removeClass('disabled').removeAttr("disabled");
    }
}

$('#view-events-date').datepicker();
$('#view-events-time').timepicker({
    minuteStep: 1,
    showSeconds: false,
    showMeridian: false
});

//initial opening of modal
$('.events-view').click(function(e){
    e.preventDefault();
    openViewEventsModal();
});

$('#view-events-all-btn').click(function(){
    toggleViewEventsUI(false);
    toggleFilterUI(true);
    
    if ($('#view-events-filter-date').hasClass('active')) {
        getAllEvents("date");
    } else {
        getAllEvents('incidents');
    }
});

$('#view-events-filter-btn').click(function(){
    toggleViewEventsUI(false);
    toggleFilterUI(false);
    
    if ($('#view-events-filter-date').hasClass('active')) {
        getEventsForDate("date");
    } else {
        getEventsForDate('incidents');
    }
});

$('#view-events-filter-date').click(function(){
    if (!$('#view-events-filter-date').hasClass('active')) {
        toggleViewEventsUI(false);
        if ($('#view-events-filter-btn').hasClass('active')) {
            getEventsForDate('date');
        } else {
            getAllEvents('date');
        }
    }
});

$('#view-events-filter-incidents').click(function(){
    if (!$('#view-events-filter-incidents').hasClass('active')) {
        toggleViewEventsUI(false);
        if ($('#view-events-filter-btn').hasClass('active')) {
            getEventsForDate('incidents');
        } else {
            getAllEvents('incidents');
        }
    }
});