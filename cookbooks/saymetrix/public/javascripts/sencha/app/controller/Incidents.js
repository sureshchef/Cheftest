Ext.define('SF.controller.Incidents', {
    extend: 'Ext.app.Controller',

    stores: [
    'ListIncidents', 'MapIncidents', 'MapSites'
    ],

    models: [
    'Incident'
    ],

    views: [
    'incident.List', 'incident.Detail', 'incident.Map'
    ],

    refs: [
    {
        ref: 'incidentMap', 
        selector:'incidentmap'
    },

    {
        ref: 'incidentList', 
        selector:'incidentlist'
    }
    ],

    mapReady: false,
    dataLoaded: false,
    initialSearch: true,
    idOfItemClicked: 0,
    idOfMarkerClicked: 0,
    zooming: false,
    layersLoaded: false,
 
    init: function() {
        this.getListIncidentsStore().addListener('load', this.onListDataLoad, this);

        this.control({
            'incidentlist': {
                itemclick: this.onListItemClick
            },
            'incidentmap': {
                markerclick: this.onClickMarker,
                markersloaded: this.onMarkersLoaded,
                clusteringend: this.onClusteringEnd,
                mapclick: this.onMapClick,
                zoomchange: this.onZoomChange
            },
            'tabs': {
                searching: this.clearSelected
            },
            'newsearch': {
                searchdata: this.onSearchData
            },
            'editsearch': {
                searchdata: this.onSearchData
            }
        });
    },

    onLaunch: function() {
        this.getIncidentMap().addListener('mapready', this.onMapReady, this);
    },

    //called when the mapready event is fired by the google map
    onMapReady: function() {
        this.mapReady = true;

        if (!preferredLocation && navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(function(position) {
            Ext.getCmp('map').getMap().setCenter(new google.maps.LatLng(position.coords.latitude,
                position.coords.longitude));
          });
        }

        this.getIncidentMap().addCustomListeners()
        this.onDataLoadedMapReady();
    },

    //called whenever data is loaded into the ListIncidents store
    onListDataLoad: function() {
        this.dataLoaded = true;
        var theStore = Ext.getStore('ListIncidents');
        var number = theStore.getTotalCount();
        Ext.getCmp('tabs').updateActiveTabResultsCount(number);
        if (this.idOfMarkerClicked > 0) {
            this.fromMarkerToList(this.getIncidentList(), theStore, this.idOfMarkerClicked);
        } else {
            this.onDataLoadedMapReady();
        }
    },

    //called by either onMapReady or onListDataLoad
    onDataLoadedMapReady: function() {
        if(this.dataLoaded && this.mapReady) {
            var map = this.getIncidentMap();
            if (this.initialSearch) {
                map.loadViewportMarkers();
                this.initialSearch = false;
            }
            if(map.cache.marker.length > 0) {
                var point = map.cache.marker[0].position;
                map.getMap().setCenter(point, map.zoomLevel);
            }
            
            if (!this.layersLoaded) {
                this.loadCustomLayers();
                this.layersLoaded = true;
            }
        }
    },
    
    //called whenever the searchdata event is fired by the New Search or Edit Search tabs. 
    onSearchData: function(loadObject) {
        this.clearSelected();
        Ext.getStore('ListIncidents').load(loadObject);
        if (this.mapReady) {
            this.getIncidentMap().loadViewportMarkers();
        }
    },
    
    onListItemClick: function(view, record, item, index, e) {
        if (this.idOfItemClicked == record.getData().id) {
            this.onMarkersLoaded();
        } else {
            this.onMapClick();
            var map = this.getIncidentMap();
            var point = new google.maps.LatLng(record.raw.latitude, record.raw.longitude);
            map.getMap().setCenter(point, map.zoomLevel);
            this.idOfItemClicked = record.getData().id;
        }
    },
    
    //called when the markersloaded event is fired, indicating that the map has finished loading markers, map is typically in an "idle" state at this point
    onMarkersLoaded: function() {
        //we know zooming has stopped because the idle event was fired
        this.zooming = false;
        if (this.idOfItemClicked > 0) {
            this.onClickMarker(this.idOfItemClicked);
            this.idOfItemClicked = 0;
        }
    },

    onClickMarker: function(markerId) {
        if (markerId != this.idOfMarkerClicked) {
            var listView = this.getIncidentList();
            var theStore = this.getListIncidentsStore();
            var rowIndex = theStore.find('id', markerId);
            this.idOfMarkerClicked = markerId;
            if (rowIndex < 0) {
                var tab = Ext.getCmp('tabs').getActiveTab();
                if (tab.xtype == "filtertab") {
                    tab = Ext.getCmp('newSearch');
                }
                tab.validateSearch();
                if (tab.getIsValid()) {
                    Ext.Ajax.request({
                        url: rootFolder+'/api/paginatedPageNumber?limit='+theStore.pageSize+'&sort='+theStore.sorters.items[0].property+'&dir='+theStore.sorters.items[0].direction+'&incident.id='+markerId,
                        jsonData: tab.getFilterAsJson(),
                        method: 'POST',
                        success: function(response) {
                            var jsonResponse = Ext.decode(response.responseText);
                            theStore.loadPage(jsonResponse.pagenum);
                        },
                        failure: function(response, opts) {
                            console.log('Server-side failure with status code ' + response.status);
                            Ext.Msg.alert('Failure', 'Server-side failure with status code ' + response.status);
                        }
                    });
                }
            } else {
                this.fromMarkerToList(listView, theStore, markerId);
            }
        } else {
            this.clearSelected();
        }
    },
    
    fromMarkerToList: function(listView, theStore, markerId) {
        var rowIndex = theStore.find('id', markerId);
        var record = this.getListIncidentsStore().getById(markerId);
        listView.getSelectionModel().select(record);
        this.onItemClick(listView, record, {}, rowIndex, {});
    },

    onItemClick: function(view, record, item, index, e) {
        Ext.getCmp('details').update(record.getData());
        var bubbleToOpen;
        for(var i = 0; i < this.getIncidentMap().bubbles.length; i++) {
            var bubbleToClose = this.getIncidentMap().bubbles[i];
            if (bubbleToClose.marker.id == record.getData().id){
                bubbleToOpen = bubbleToClose;
            }
            bubbleToClose.wasClicked = false;
            bubbleToClose.close();
        }
        if (bubbleToOpen) {
            bubbleToOpen.wasClicked = true;
            var marker = bubbleToOpen.marker;

            /**
            *  HACK: Clusters not refreshing when zooming in on the map.  
            *  Clicking on the list will result in the markers_.length to be undefined 
            *  and exits the function.  The try-catch helps but is a quick fix.
            */
           
           /**
            * UPDATE: 18/10/12 by Gordon
            * This problem is due to the fact that when you zoom out by what appears to be
            * one level, the zoom_changed event gets sent out several times.This would cause the
            * map to render on each event, but the clusterer would not render until the very end,
            * thus causing the exception.
            * This problem has been solved by simply making sure this function doesn't execute until
            * after the zooming has finished. The try-catch has been removed.
            */
            if (marker.myCluster) {
                // this code will make sure that a info bubble for a marker inside a cluster will appear to come from the cluster
                if (marker.myCluster.markers_) {
                    if (marker.myCluster.markers_.length > 1) {
                        marker = marker.myCluster.markers_[0];
                        marker.anchorPoint = marker.myCluster.clusterIcon_.center_;
                    }
                }
            }
            Ext.getCmp('map').bubbleToKeepOpen = bubbleToOpen;
            if (!Ext.getCmp('map').enableMarkers) {
                Ext.getCmp('map').bubbleToKeepOpen.marker.setMap(this.getIncidentMap().getMap());
                Ext.getCmp('map').bubbleToKeepOpen.marker.setVisible(true);
            }
            bubbleToOpen.open(this.getIncidentMap().getMap(), marker);
        }
    },
    
    //clears selection of markers, list items, and empties the details panel.
    clearSelected: function() {
        //close all info bubbles
        for(var i = 0; i < this.getIncidentMap().bubbles.length; i++) {
            var bubbleToClose = this.getIncidentMap().bubbles[i];
            bubbleToClose.wasClicked = false;
            bubbleToClose.close();
        }
        this.getIncidentList().getSelectionModel().deselectAll();
        Ext.getCmp('details').update({});
        this.idOfItemClicked = 0;
        this.idOfMarkerClicked = 0;
        if (Ext.getCmp('map').bubbleToKeepOpen) {
            if (!Ext.getCmp('map').enableMarkers) {
                Ext.getCmp('map').bubbleToKeepOpen.marker.setMap(null);
            }
            Ext.getCmp('map').bubbleToKeepOpen = null;
        }
    },
    
    //called when the markerclusterer has finished rendering the clusters (may get sent several times during a zoom)
    onClusteringEnd: function() {
        if (!this.zooming && Ext.getCmp('map').enableMarkers) {
            //set markers to be visible
            for (var i = 0; i< this.getIncidentMap().cache.marker.length; i++) {
                this.getIncidentMap().cache.marker[i].setVisible(true);
            }
            if (this.idOfMarkerClicked > 0) {
                var theStore = Ext.getStore('ListIncidents');
                this.fromMarkerToList(this.getIncidentList(), theStore, this.idOfMarkerClicked);
            }
        }
    },
    
    //fires whenever the map is clicked (doesn't fire if click is on marker, cluster, etc.)
    onMapClick: function() {
        this.clearSelected();
    },
    
    //gets sent out after a zoom has occured (is the first event to fire after a zoom)
    onZoomChange: function() {
        this.zooming = true;
    },
    
    loadCustomLayers: function() {
        var map = this.getIncidentMap();
        var layers = [];
        var that = this;
        //Markers Layer
        var markerlayer = {
            enabled: true,
            enable: function() {
                if (Ext.getCmp('map').bubbleToKeepOpen) {
                    Ext.getCmp('map').bubbleToKeepOpen.marker.setMap(null);
                }
                Ext.getCmp('map').enableMarkers = true;
                this.enabled = true;
                if (Ext.getCmp('map').boundsChanged) {
                    Ext.getCmp('map').loadViewportMarkers();
                } else {
                    Ext.getCmp('map').addClusterer();
                    Ext.getCmp('map').showMarkers();
                }
            },
            disable: function() {
                Ext.getCmp('map').enableMarkers = false;
                this.enabled = false;
                Ext.getCmp('map').hideMarkers();
                if(Ext.getCmp('map').markerClusterer) {
                    Ext.getCmp('map').markerClusterer.clearMarkers();
                }
                
                for(var i = 0; i < Ext.getCmp('map').bubbles.length; i++) {
                    var bubble = Ext.getCmp('map').bubbles[i];
                    if (bubble.marker.id == that.idOfMarkerClicked){
                        Ext.getCmp('map').bubbleToKeepOpen = bubble;
                        Ext.getCmp('map').bubbleToKeepOpen.marker.setMap(Ext.getCmp('map').getMap());
                        Ext.getCmp('map').bubbleToKeepOpen.open(Ext.getCmp('map').getMap(), Ext.getCmp('map').bubbleToKeepOpen.marker);
                    }
                }
            }
        };
        layers.push({name: "Incidents", layer: markerlayer, type: 'markers', alwaysShow: true, enabled:true});
        
        var siteslayer = {
            enabled: false,
            enable: function() {
                this.enabled = true;
                Ext.getCmp('map').showSiteMarkers();
            },
            disable: function() {
                this.enabled = false;
                Ext.getCmp('map').hideSiteMarkers();
            }
        };
        layers.push({name: "Network Sites", layer: siteslayer, type: 'markers', alwaysShow: true, enabled:false});
        
        var groupLayerOne;
        var groupLayerTwo;
        switch(lrgrp) {
        case 1:
            var coverage2g = new google.maps.KmlLayer('http://saymetrix.danutech.com/dl/viva_201302_2G.kmz?'+(new Date()).getTime(), {   //date/time bit is to get around google's caching
                suppressInfoWindows: true,
                preserveViewport: true
            });
            var coverage3g = new google.maps.KmlLayer('http://saymetrix.danutech.com/dl/viva_201302_3G.kmz?'+(new Date()).getTime(), {   //date/time bit is to get around google's caching
                suppressInfoWindows: true,
                preserveViewport: true
            });
            groupLayerOne = {name: "2G", layer: coverage2g, type: 'layer', defaultOption: false};
            groupLayerTwo = {name: "3G", layer: coverage3g, type: 'layer', defaultOption: true };
        break;
        default:
            var exchangeslayer = new google.maps.KmlLayer('http://saymetrix.danutech.com/dl/exchanges.kmz?'+(new Date()).getTime(), {   //date/time bit is to get around google's caching
                suppressInfoWindows: true,
                preserveViewport: true
            });
            layers.push({name: "Exchange Boundaries", layer: exchangeslayer, type: 'layer', alwaysShow: true, enabled:false});

            //Create Coverage Layer Group           //If no layers are supplied with the group, errors occur, but really the group should not be created if it has no layers (right?)
            var hsdpaLayer = new CustomTileLayer(map.getMap(), 1, 22);
            hsdpaLayer.baseURL = 'http://82.196.210.230/CP/map/tile?services=HSDPA_072&qualities=2,4&serviceGroup=mobile_broadband'
            var opt = hsdpaLayer.getOverlayOptions();
            var imagemap = new google.maps.ImageMapType(opt);
            var gsmLayer = new CustomTileLayer(map.getMap(), 1, 22);
            gsmLayer.baseURL = 'http://82.196.210.230/CP/map/tile?services=GSM_VOICE&qualities=2,4&serviceGroup=voice_text';
            opt = gsmLayer.getOverlayOptions();
            var imagemaptwo = new google.maps.ImageMapType(opt); //b5 is a special variable for this type of overlay found inside ImageOverlay.js
            groupLayerOne = {name: "3G Data", layer: imagemap, type: 'imageOverlay', defaultOption: true};
            groupLayerTwo = {name: "2G Voice", layer: imagemaptwo, type: 'imageOverlay', defaultOption: false};
        }
        //groups have slightly different syntax
        layers.push({id: "coverage", name: "Network Coverage", layers: [groupLayerOne, groupLayerTwo], type: 'group', alwaysShow: true, enabled: false});
        
        map.addCustomLayerControls(layers);
    }
});