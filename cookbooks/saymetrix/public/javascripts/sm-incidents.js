//Common functions
var centerLatLng;

//Create Incident Stuff
var createMap, createMarker;
var subscriberExists = false;
var editingIncident = false;
var editNetEventId = 0;
var editingWithEvent = false;

function initialiseIncidentCreateMap(googleMapOptions) {
    //create search box
    var input = document.getElementById('locationSearch');
    var searchBox = new google.maps.places.SearchBox(input);
    
    createMap = new google.maps.Map(document.getElementById("locationMap"), googleMapOptions);
    
    //create marker
    var shadow = new google.maps.MarkerImage(rootFolder+"/public/images/map-icons/markers/marker-shadow.png",
        new google.maps.Size(76.0, 50.0),
        new google.maps.Point(0, 0),
        new google.maps.Point(27.0, 50.0)
    );
    createMarker = new google.maps.Marker({
        position:createMap.getCenter(),
        map: createMap,
        draggable:true,
        animation: google.maps.Animation.DROP,
        title:"",
        shadow: shadow,
        icon: rootFolder+"/public/images/map-icons/markers/Other.png"
    });
    
    //go get the geolocation info
    setGeoLocation(centerLatLng);
    
    //then set up listeners
    google.maps.event.addListener(searchBox, 'places_changed', function() {
        var places = searchBox.getPlaces();
        createMarker.setPosition(places[0].geometry.location);
        createMap.setCenter(places[0].geometry.location);
        createMarker.setTitle(places[0].formatted_address);
    });
    google.maps.event.addListener(createMarker, 'dragend', function(e) {
        setGeoLocation(e.latLng);
    });
}

function openIncidentModal(date) {
    $('#create-incident').modal('show');
    $('#create-incident .spinner').hide();
    $('.create-date').datepicker('setValue', date);  //need to set date here so that its filled when user comes to it
    $('#timepicker').timepicker('setTime', date.toTimeString());  //need to set time here so its the most up to date
}

function openNewIncident(date) {
    if (!date){
        date = new Date();
    }
    openIncidentModal(date);
    $('#create-subscriber label[for=firstname]').addClass("muted");
    $('#create-subscriber input[name=firstname]').attr("disabled", true);
    $('#create-subscriber label[for=lastname]').addClass("muted");
    $('#create-subscriber input[name=lastname]').attr("disabled", true);
    $('#create-subscriber label[for=email]').addClass("muted");
    $('#create-subscriber input[name=email]').attr("disabled", true);
    $('#incident-create').addClass("disabled");
    showSubscriberTab();
}

function showSubscriberTab() {
    $('#create-tabs a[href="#create-subscriber"]').tab('show');
    $('#incident-back').addClass("disabled");
    $('#incident-create').text("Next");
    $('#create-incident .modal-header h3 small').text('- Subscriber Details');
}

function showIncidentDetailsTab() {
    $('#create-tabs a[href="#create-details"]').tab('show');
    $('#incident-back').removeClass("disabled");
    $('#incident-create').removeClass("disabled");
    $('#incident-create').text("Next");
    $('#create-incident .modal-header h3 small').text('- Incident Details');
}

function showIncidentLocationTab() {
    $('#create-tabs a[href="#create-location"]').tab('show');
    $('#incident-back').removeClass("disabled");
    $('#create-incident .modal-header h3 small').text('- Incident Location');
    refreshCreateMap();
    switchIncidentMarkerIcon(); //possibly move this as a callback on the select2 plugin
}

function showNetEventsTab() {
    $('#create-tabs a[href="#create-netevent"]').tab('show');
    if (editingIncident) {
        $('#incident-create').text("Update");
    } else {
        $('#incident-create').text("Create");
    }
    $('#create-incident .modal-header h3 small').text('- Associate With Network Event');
    toggleUI('incident', false);
    getEventsForIncident();
}

function addIncidentEventToList(event) {
    var start = getReadableDate(event.eventPeriod.startMillis);
    var end = getReadableDate(event.eventPeriod.endMillis);
    var desc = event.description;
    if (event.description.length > 75) {
        desc = event.description.substring(0, 75) + "...";
    }
    var radiocheck = '';
    if (editNetEventId > 0 && editNetEventId == event.id) {
        radiocheck = 'checked="checked"';
        editNetEventId = 0;
    }
    $('#select-events-list').append('<div class="view-event" onclick="selectEvent(this);"><div class="pull-left"><input type="radio" name="netevent" value="'+event.id+'" '+radiocheck+' /></div><div class="content"><h3>'+event.eventType.name+' <small>- '+event.subject+'</small></h3><h6>'+start+' - '+end+'</h6><p>'+desc+'</p></div></div>');
}

function selectEvent(el) {
    $(el).find('input[name=netevent]').attr('checked', true);
}

function getEventsForIncident() {
    var selectedDate = $('.create-date input').val();
    var utcTime = new Date(selectedDate.substring(3, 6) + selectedDate.substring(0, 3) + selectedDate.substring(6) + " " + $('#timepicker').val()).toISOString();
    $.ajax({
        type : 'GET',
        url : rootFolder+'/api/events-list/'+utcTime+'?sort=date',
        dataType : 'json'
    }).done(function(events) {
        $('#select-events-list').html("");
        if (events.length > 0) {
            var radiocheck = '';
            if (!editNetEventId || editNetEventId == 0) {
                radiocheck = 'checked="checked"';
            }
            $.each(events, function(index,event){
                addIncidentEventToList(event);
            });
            $('#select-events-list').prepend('<div class="view-event" onclick="selectEvent(this);"><div class="pull-left"><input type="radio" name="netevent" value="0" '+radiocheck+' /></div><div class="content"><h3>None</h3><h6>This incident was not cause by an event.</h6></div></div>');
        } else {
            $('#select-events-list').append('<div class="events-list-holder"><h3>No Network Events occured during this date</h3><h4>'+$('.create-date input').val()+' at '+$('#timepicker').val()+'</h4></div>');
        }
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
    }).always(function() {
        //switch ui back on
        toggleUI('incident', true);
    });
}

function refreshCreateMap() {
    google.maps.event.trigger(createMap, 'resize');
    if (createMarker.getPosition()) {   //is this if statement needed? should just use createMarker.getPosition()?
        createMap.setCenter(createMarker.getPosition());
    } else {
        createMap.setCenter(new google.maps.LatLng(latitude, longitude));
    }
}

function setGeoLocation(latLng) {
    var createGeocoder = new google.maps.Geocoder();
    createGeocoder.geocode({'latLng': latLng}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            if (results[0]) {
                createMarker.setTitle(results[0].formatted_address);
                $('#locationSearch').val(results[0].formatted_address);
            } else {
                createMarker.setTitle(latLng.toString());
            }
        } else {
            createMarker.setTitle(latLng.toString());
        }
    });
}

function switchIncidentMarkerIcon() {
    if ($('#create-incidents').select2("val").indexOf("voice") !== -1) {
        createMarker.setIcon(rootFolder+"/public/images/map-icons/markers/Voice.png");
    } else if ($('#create-incidents').select2("val").indexOf("data") !== -1) {
        createMarker.setIcon(rootFolder+"/public/images/map-icons/markers/Data.png");
    } else {
        createMarker.setIcon(rootFolder+"/public/images/map-icons/markers/Other.png");
    }
}

function createSubscriber() {
    $.ajax({
		type : 'POST',
		url : $('#create-subscriber form').attr('action'),
		data : $('#create-subscriber form').serialize()+'&account=NONE',
		dataType : 'json'
	}).always(function() {
		$('#incident-create').removeClass('disabled')
	}).done(function() {
		showIncidentDetailsTab();
	}).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
        var response = $.parseJSON(obj.responseText);
        if (obj.status == 400) {
            addErrorToInput('#create-incident input[name="msisdn"]', response["s.msisdn"]);
            addErrorToInput('#create-incident input[name="firstname"]', response["s.firstname"]);
            addErrorToInput('#create-incident input[name="lastname"]', response["s.lastname"]);
            addErrorToInput('#create-incident input[name="email"]', response["s.email"]);
        }
	});
}

function postIncidentData() {
    toggleUI('incident', false);
    //hack for getting time/date string into UTC and in format that Server will like
    var selectedDate = $('.create-date input').val();
    var utcTime = new Date(selectedDate.substring(3, 6) + selectedDate.substring(0, 3) + selectedDate.substring(6) + " " + $('#timepicker').val()).toISOString();
    
    //get the data and ajax it
    var formData = {
        "msisdn": $('#sub-msisdn').val(),
        "date": utcTime,
        "incidentType": $('#create-incidents').select2("val"),
        "frequency": $('#create-frequency').select2("val"),
        "position": $('#create-position').select2("val"),
        "description": $('#create-incident textarea').val(),
        "phoneType": $('#create-incident input[name=phoneType]').val(),
        "phoneOs": $('#create-incident input[name=phoneOs]').val(),
        "lat": createMarker.getPosition().lat(),
        "lng": createMarker.getPosition().lng(),
        "eventId": $('#create-netevent input:radio[name=netevent]:checked').val() || 0
    };
    var ajaxType = 'POST';
    var urlAddOn = '';
    if (editingIncident) {
        ajaxType = 'PUT';
        urlAddOn = '/'+$('#create-incident input[name=id]').val();
    }
    $.ajax({
        type : ajaxType,
        url : $('#create-location form').attr('action')+urlAddOn,
        data: JSON.stringify(formData),
        contentType: "application/json; charset=utf-8",
        dataType : 'json'
    }).always(function() {
        toggleUI('incident', true);
    }).done(function(obj) {
        if (obj) {
            $('#create-details input[name=id]').val(obj);
        }
        $('#create-incident').modal('hide');
        //show some sort of success message
        if (editingIncident) {
            Ext.MessageBox.alert('Success', 'Incident successfully updated.');
            var tab = Ext.getCmp('tabs').getActiveTab();
            if (tab.xtype == "filtertab") {
                tab = Ext.getCmp('newSearch');
            }
            tab.submitSearch();
        } else {
            Ext.MessageBox.alert('Success', 'Incident successfully created.');
        }
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
        var response = $.parseJSON(obj.responseText);
        if (obj.status == 400) {
            addErrorToInput('#create-incident .create-date', response["i.date"]);
            addErrorToInput('#create-incident select[name="incidentType"]', response["i.incidentType"]);
            addErrorToInput('#create-incident select[name="frequency"]', response["i.frequency"]);
            addErrorToInput('#create-incident select[name="position"]', response["i.position"]);
            addErrorToInput('#create-incident textarea[name="description"]', response["i.description"]);
            addErrorToInput('#create-incident input[name="location"]', response["i.location"]);
            showIncidentDetailsTab();
        } else if (obj.status == 500) {
            Ext.MessageBox.alert(response.heading, response.message);
        }
    });
}

function updateIncidentWithEvent() {
    toggleUI('incident', false);
    var formData = {
        "incidentId": $('#create-details input[name=id]').val(),
        "eventId": $('#create-netevent input:radio[name=netevent]:checked').val()
    };
    $.ajax({
        type : 'POST',
        url : $('#create-netevent form').attr('action'),
        data: JSON.stringify(formData),
        contentType: "application/json; charset=utf-8",
        dataType : 'json'
    }).always(function() {
        toggleUI('incident', true);
    }).done(function(e) {
        $('#create-incident').modal('hide');
        //show some sort of success message
        Ext.MessageBox.alert('Success', 'Incident successfully added to Network Event.');
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
        var response = $.parseJSON(obj.responseText);
        if (obj.status == 400) {
            
        } else if (obj.status == 500) {
            Ext.MessageBox.alert(response.heading, response.message);
        }
    });
}

function confirmModal() {
    if (editingWithEvent) {
        updateIncidentWithEvent();
    } else {
        postIncidentData();
    }
}

function editIncident(id) {
    $.ajax({
        type : 'GET',
        url : rootFolder+'/api/incidents/'+id,
        dataType : 'json'
    }).done(function(obj) {
        //date and time
        var iDate = new Date(fromISOString(obj.date));
        openIncidentModal(iDate);
        showIncidentDetailsTab();
        $('#incident-back').addClass('disabled');
        $('#create-incident .modal-header h3 span').text("Edit Incident");
        editingIncident = true;
        //fill out inputs
        $('#create-details input[name=id]').val(id);
        $('#create-incidents').select2("val", obj.incidentType.key);
        $('#create-frequency').select2("val", obj.frequency);
        $('#create-position').select2("val", obj.position);
        $('#create-incident textarea[name=description]').val(obj.comment);
        $('#create-incident input[name=phoneType]').val(obj.phoneType);
        $('#create-incident input[name=phoneOs]').val(obj.phoneOs);
        if (obj.networkEvent) {
            editNetEventId = obj.networkEvent.id;
        }
        //set latlng of map and marker
        var point = new google.maps.LatLng(obj.latitude, obj.longitude);
        createMarker.setPosition(point);
        createMap.setCenter(point);
        setGeoLocation(point);
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
    });
}

function associateIncidentWithEvent(id) {
    $.ajax({
        type : 'GET',
        url : rootFolder+'/api/incidents/'+id,
        dataType : 'json'
    }).done(function(obj) {
        if (obj.networkEvent) {
            editNetEventId = obj.networkEvent.id;
        }
        editingIncident = true;
        editingWithEvent = true;
        //date and time
        var iDate = new Date(fromISOString(obj.date));
        openIncidentModal(iDate);
        showNetEventsTab();
        $('#incident-back').addClass('disabled');
        $('#create-incident .modal-header h3 span').text("Edit Incident");
        //fill out inputs
        $('#create-details input[name=id]').val(id);
        $('#incident-create').removeClass('disabled');
    }).fail(function(obj, err, msg) {
        console.log(obj, err, msg);
    });
}

//initialise everything else in the modal
$('.create-date').datepicker();
$('#timepicker').timepicker({
    minuteStep: 1,
    showSeconds: true,
    showMeridian: false
});
$('#create-incidents').select2({
    placeholder : "Select an Incident Type"
});
$('#create-frequency').select2().select2("val", "ONCE");
$('#create-position').select2();

$('#move-pin').click(function() {
    createMarker.setPosition(createMap.getCenter());
    setGeoLocation(createMap.getCenter());
});

//initial opening of modal
$('.incident-create').click(function(e){
    e.preventDefault();
    openNewIncident();
});

$('#incident-create').click(function(e) {
    if (!$('#incident-create').hasClass('disabled')) {
        if ($('#create-subscriber').hasClass("active")) {
            if (subscriberExists) {
                showIncidentDetailsTab();
            } else {
                $('#incident-create').addClass('disabled')
                createSubscriber();
            }
        } else if ($('#create-details').hasClass("active")) {
            var moveon = true;
            if ($('.create-date input').val() == "" || $('#timepicker').val() == "") {
                moveon = false;
                addErrorToInput('#create-incident .create-date', "Required");
            }
            if ($('#create-incidents').select2("val") == "") {
                moveon = false;
                addErrorToInput('#create-incident select[name="incidentType"]', "Required");
            }
            if (moveon) {
                showIncidentLocationTab();
            }
        } else if ($('#create-location').hasClass("active")) {
            showNetEventsTab();
        } else if ($('#create-netevent').hasClass("active")) {
            confirmModal();
        }
    }
});

//back button clicking
$('#incident-back').click(function(e) {
    if (!$('#incident-back').hasClass('disabled')) {
        if ($('#create-details').hasClass("active")) {
            showSubscriberTab();
        } else if ($('#create-location').hasClass("active")) {
            showIncidentDetailsTab();
            if (editingIncident) {
                $('#incident-back').addClass('disabled');
            }
        } else if ($('#create-netevent').hasClass("active")) {
            showIncidentLocationTab();
        }
    }
});

//sort of a hack to get the tabs in the modal to render properly when re-opened in IE7
$('#create-incident').on('hide', function(e) {
    //need to check source, as the datepicker also fires this event
    if ($(e.target).hasClass("incidentModal")) {
        showSubscriberTab();
    }
});

//When modal is removed from view...
$('#create-incident').on('hidden', function() {
    $('#create-incident .modal-body .help-inline').html("");
    $('#check-btn .help-block').html('');
    $('#create-incident .modal-body').find('.error').removeClass('error');
    $('#create-incident .modal-body').find('.warning').removeClass('warning');
    $('#create-incident .modal-body').find('.success').removeClass('success');
    $('#create-incident .modal-body input').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
    $('#create-incident .modal-body textarea').val("").trigger('blur.placeholder'); //this event is part of the placeholder plugin. If plugin is not used, the event trigger will do nothing.
    $('#create-incidents').select2("val", "");
    $('#create-frequency').select2("val", "ONCE");
    $('#create-position').select2("val", "INDOOR");
    createMap.setCenter(centerLatLng);
    createMarker.setPosition(centerLatLng);
    setGeoLocation(centerLatLng);
    if (editingIncident) {
        $('#create-incident .modal-header h3 span').text("Report Incident");
        editingIncident = false;
    }
    editingWithEvent = false;
});

$('#check-btn button').click(function() {
    if (!$('#check-btn button').hasClass('disabled')) {
        $('#check-btn button').addClass('disabled');
        $('#check-btn .spinner').show();
        $('#check-btn').closest('.control-group').removeClass("success");
        $('#check-btn').closest('.control-group').removeClass("warning");
        $('#check-btn').closest('.control-group').removeClass("error");
        $('#check-btn .help-block').html('');
        $('#create-subscriber label[for=firstname]').addClass("muted");
        $('#create-subscriber input[name=firstname]').attr("disabled", true);
        $('#create-subscriber label[for=lastname]').addClass("muted");
        $('#create-subscriber input[name=lastname]').attr("disabled", true);
        $('#create-subscriber label[for=email]').addClass("muted");
        $('#create-subscriber input[name=email]').attr("disabled", true);
        subscriberExists = false;
        
        if ($('#sub-msisdn').val()) {
            $.ajax({
                type : 'GET',
                url : rootFolder+'/subscribers/'+$('#sub-msisdn').val(),
                dataType : 'json'
            }).always(function() {
                $('#check-btn .spinner').hide();
                $('#check-btn button').removeClass('disabled');
                $('#incident-create').removeClass('disabled');
            }).done(function(obj) {
                $('#create-subscriber input[name=firstname]').val(obj.firstname);
                $('#create-subscriber input[name=lastname]').val(obj.lastname);
                $('#create-subscriber input[name=email]').val(obj.email);
                $('#check-btn').closest('.control-group').addClass("success");
                $('#check-btn .help-block').html('<i class="icon-ok"></i> Subscriber found.');
                subscriberExists = true;
            }).fail(function(obj, err, msg) {
                console.log(obj, err, msg);
                var response = $.parseJSON(obj.responseText);
                if (obj.status == 404) {
                    $('#check-btn button').closest('.control-group').addClass("warning");
                    $('#check-btn .help-block').html("Subscriber record not found. Please complete details below.");
                    $('#create-subscriber label[for=firstname]').removeClass("muted");
                    $('#create-subscriber label[for=lastname]').removeClass("muted");
                    $('#create-subscriber label[for=email]').removeClass("muted");
                    $('#create-subscriber input[name=firstname]').removeAttr("disabled").val("");
                    $('#create-subscriber input[name=lastname]').removeAttr("disabled").val("");
                    $('#create-subscriber input[name=email]').removeAttr("disabled").val("");
                } else if (obj.status == 500) {
                    Ext.MessageBox.alert(response.heading, response.message);
                }
            });
        } else {
            $('#check-btn .spinner').hide();
            $('#check-btn button').removeClass('disabled');
            $('#check-btn').closest('.control-group').addClass("error");
            $('#check-btn .help-block').html('Please enter a value.');
        }
    }
});


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
    
    initialiseIncidentCreateMap(googleMapOptions);
    if (typeof initialiseEventCreateMap == 'function') { 
        initialiseEventCreateMap(googleMapOptions);
    }
    
}
google.maps.event.addDomListener(window, 'load', initializeMaps);
