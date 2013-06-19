Ext.define('SF.view.incident.Map', {
    extend: 'Ext.ux.GMapPanel',
    alias: 'widget.incidentmap',

    store: ['MapIncidents', 'MapSites'],
    
    mixins: {
        overlays: 'SF.mixin.Overlays',
        sitemarkers: 'SF.mixin.SiteMarkers'
    },
    
    idleSkipped: false,
    boundsChanged: true,
    isMapDragging: false,
    enableMarkers: false,
    bubbleToKeepOpen: null,
 
    initComponent: function() {
        this.addEvents('markerclick', 'markerover', 'clusteringend', 'mapclick', 'zoomchange');
        this.callParent(arguments);
    },
    
    addCustomListeners: function() {
        //events for signaling when to load new markers when the map is moved in some way
        google.maps.event.addListener(this.getMap(), 'idle', Ext.Function.bind(function() {
            if (Ext.getCmp('map').isMapDragging) {
                Ext.getCmp('map').idleSkipped = true;
                return;
            }
            Ext.getCmp('map').idleSkipped = false;
            Ext.getCmp('map').loadViewportMarkers();
        }, this));
        
            //want to make sure the idle event doesn't continuously fire if we stop briefly when moving the map'
        google.maps.event.addListener(this.getMap(), 'dragstart', Ext.Function.bind(function() {
            Ext.getCmp('map').isMapDragging = true;
        }, this));
        
        google.maps.event.addListener(this.getMap(), 'dragend', Ext.Function.bind(function() {
            Ext.getCmp('map').isMapDragging = false;
            if (Ext.getCmp('map').idleSkipped == true) {
                Ext.getCmp('map').loadViewportMarkers();
                Ext.getCmp('map').idleSkipped = false;
            }
        }, this));
        
        google.maps.event.addListener(this.getMap(), 'bounds_changed', Ext.Function.bind(function() {
            Ext.getCmp('map').idleSkipped = false;
            Ext.getCmp('map').boundsChanged = true;
        }, this));
        
        //events for when to clear any info bubbles from the map markers, based on map events
        google.maps.event.addListener(this.getMap(), 'click', Ext.Function.bind(function() {
            Ext.getCmp('map').fireEvent('mapclick');
        }, this));
        
        google.maps.event.addListener(this.getMap(), 'zoom_changed', Ext.Function.bind(function() {
            Ext.getCmp('map').fireEvent('zoomchange');
        }, this));
        
        //event for after loading the data to the store
        Ext.getStore('MapIncidents').addListener('load', this.updateMap, this);
        Ext.getStore('MapSites').addListener('load', this.loadSiteMarkers, this);
    },
    
    loadViewportMarkers : function(){
        var tab = Ext.getCmp('tabs').getActiveTab();
        if (tab.xtype == "filtertab") {
            tab = Ext.getCmp('newSearch');
        }
        
        var loadingMask = new Ext.LoadMask(Ext.getCmp('map').el, {msg:"Loading ..."});
        
        tab.validateSearch();
        if (tab.getIsValid()) {
            var bounds = this.getMap().getBounds().toUrlValue();
            if(this.enableMarkers) {
                loadingMask.show();
                Ext.Ajax.request({
                    url: rootFolder+'/api/map-incidents.json?bounds='+bounds.toString(),
                    jsonData: tab.getFilterAsJson(),
                    method: 'POST',
                    success: function(response) {
                        var data = Ext.decode(response.responseText);
                        Ext.getStore('MapIncidents').loadRawData(data);
                        loadingMask.hide();
                        Ext.getCmp('map').boundsChanged = false;
                    },
                    failure: function(response, opts) {
                        loadingMask.hide();
                        if (status != 0) {
                            Ext.Msg.alert('Failure', 'Server-side failure with status code ' + response.status);
                        }
                    }
                });
            }
        }
        this.loadViewportSites();
    },

    updateMap: function() {
        //close any open bubbles before updating (fixes bug where if you move map while clicking a marker)
        for(var i = 0; i < this.bubbles.length; i++) {
            this.bubbles[i].close();
        }
        
        var records = Ext.getStore('MapIncidents').data.items;
        var length = records.length;
        var myArray = [];
        var record, marker;
        
        for (var i = 0; i < length; i++) {
            record = records[i].data;
            var date = Ext.Date.format(record.date, 'd/m/Y H:i:s');
            marker = {
                lat: record.latitude,
                lng: record.longitude,
                marker: {
                    title: record.incidentType, 
                    type:record.incidentGroup,
                    id: record.id, 
                    infoBubble: {
                        content:'<div class="infoBubbleHeader"><h3>'+record.incidentType+' <small>&ndash; '+record.incidentGroup+'</small></h3></div><div class="infoBubbleFooter"><p>'+date+'</p></div>'
                    }
                },
                listeners: {
                    click: function() {
                        Ext.getCmp('map').onClickMarker(this.id);
                    }
                }
            }
            
            myArray.push(marker);
        }

        this.cache.marker.length = 0;
        this.bubbles.length = 0;
        this.addMarkers(myArray);
        if (this.enableMarkers) {
            this.addClusterer();
        }
    },
    
    addClusterer: function() {
        if(this.markerClusterer) {
            this.markerClusterer.clearMarkers();
        }
        this.markerClusterer = new MarkerClusterer(this.getMap(), this.cache.marker);
        google.maps.event.addListener(this.markerClusterer, 'clusteringend', function() {
           Ext.getCmp('map').fireEvent('clusteringend');
        });
    },

    onClickMarker: function(markerId) {
        this.fireEvent('markerclick', markerId);
    }
});