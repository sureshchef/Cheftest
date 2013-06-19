//Create Network Event Stuff
var eventMap, eventMarkers, eventBubbles;
var latitude = 53.29877372565976;
var longitude = -6.178543567657471;
var centerLatLng;

function refreshEventMap() {
    google.maps.event.trigger(eventMap, 'resize');
}

function fitMapToMarkers() {
    var bounds = new google.maps.LatLngBounds();
    for (key in eventMarkers) {
        if (typeof eventMarkers[key] != 'function') {
            bounds.extend(eventMarkers[key].getPosition());
        }
    }
    eventMap.fitBounds(bounds);
};

function initialiseEventMap(googleMapOptions) {
    eventMap = new google.maps.Map(document.getElementById("event-detail-map"), googleMapOptions);
    var id = $('input[name=id]').val();
    $.ajax({
        type:"GET",
        url: rootFolder+"/api/sites/"+id
    }).done(function(sites){
        eventMarkers = [];
        eventBubbles = [];
        $.each(sites, function(index,site){
            var eventMarker = new google.maps.Marker({
                position:new google.maps.LatLng(site.latitude, site.longitude),
                map: eventMap,
                draggable:false,
                title:site.key,
                icon: rootFolder+"/public/images/map-icons/markers/measle_blue.png"
            });
            eventMarkers[site.key] = eventMarker;

            var content = '<div class="infoBubbleHeader"><h3>'+site.key+' <small>&ndash; '+site.technologies+'</small></h3></div><div class="infoBubbleFooter"><p>'+site.street_address+'</p></div>';
            createSiteBubble(eventMarker, content);
            
        });
        fitMapToMarkers();
    });
}

$(document).ready(function(){
    $(document).on('click', '.event-site', (function(e) {
        e.preventDefault();
        console.log("hello");
        var aKey = $(this).data('id');
        eventMap.setCenter(eventMarkers[aKey].getPosition());
    }));
});

function createSiteBubble(marker, bubbleContent){ 
    var infoBubble = new InfoBubble({
        map: eventMap,
        marker: marker,
        content: bubbleContent,
        position: marker.getPosition(),
        shadowStyle: 1,
        padding: 0,
        backgroundColor: '#f7f7f7',
        borderRadius: 6,
        arrowSize: 10,
        borderWidth: 1,
        borderColor: '#bbb',
        disableAutoPan: true,
        hideCloseButton: true,
        arrowPosition: 50,
        backgroundClassName: 'infoBubbleInner',
        arrowStyle: 0,
        minWidth: 150,
        maxWidth: 300,
        minHeight: 40,
        maxHeight: 100
    });

    google.maps.event.addListener(marker, 'mouseover', function() {
        if (!infoBubble.isOpen()) {
            infoBubble.open(eventMap, marker);
        }
    });
    google.maps.event.addListener(marker, 'mouseout', function() {
        if (infoBubble.isOpen()) {
            infoBubble.close();
        }
    });

    eventBubbles.push(infoBubble);
}

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
    
    initialiseEventMap(googleMapOptions);
}
google.maps.event.addDomListener(window, 'load', initializeMaps);
