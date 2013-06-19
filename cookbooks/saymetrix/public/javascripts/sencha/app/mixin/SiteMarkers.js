Ext.define('SF.mixin.SiteMarkers', {
    sites: [],
    siteBubbles: [],
    sitesEnabled: false,

    loadViewportSites : function() {
        var tab = Ext.getCmp('tabs').getActiveTab();
        if (tab.xtype == "filtertab") {
            tab = Ext.getCmp('newSearch');
        }
        
        var loadingMask = new Ext.LoadMask(Ext.getCmp('map').el, {msg:"Loading ..."});
        
        tab.validateSearch();
        if (tab.getIsValid()) {
            var bounds = this.getMap().getBounds().toUrlValue();
            if (this.sitesEnabled) {
                loadingMask.show();
                Ext.Ajax.request({
                    url: rootFolder+'/api/map-sites.json?bounds='+bounds.toString(),
                    jsonData: tab.getFilterAsJson(),
                    method: 'POST',
                    success: function(response) {
                        var data = Ext.decode(response.responseText);
                        Ext.getStore('MapSites').loadRawData(data);
                        loadingMask.hide();
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
    },

    loadSiteMarkers: function() {
        for(var i = 0; i < this.siteBubbles.length; i++) {
            this.siteBubbles[i].close();
        }
        var records = Ext.getStore('MapSites').data.items;
        var length = records.length;
        var myArray = [];
        var record, marker;
        for (var i = 0; i < length; i++) {
            record = records[i].data;
            marker = {
                record: record,
                infoBubble: {
                    content:'<div class="infoBubbleHeader"><h3>'+record.key+' <small>&ndash; '+record.technologies+'</small></h3></div><div class="infoBubbleFooter"><p>'+record.street_address+'</p></div>'
                }
            }
            
            myArray.push(marker);
        }
        
        this.clearSiteMarkers();
        this.sites.length = 0;
        this.addSiteMarkers(myArray);
    },
    
    addSiteMarkers : function(markers) {
        if (Ext.isArray(markers)){
            for (var i = 0; i < markers.length; i++) {
                this.addSiteMarker(markers[i]);
            }
        }
    },
    
    addSiteMarker : function(obj){
        Ext.applyIf(obj,{});
        
        var marker = new google.maps.Marker(Ext.apply(obj.record, {
            position: new google.maps.LatLng(obj.record.latitude, obj.record.longitude),
            icon: rootFolder+"/public/images/map-icons/markers/measle_blue.png",
            visible: true
        }));
        
        if (obj.infoBubble){
            this.createSiteBubble(marker, obj.infoBubble);
        }
        
        this.sites.push(marker);
        
        if (this.sitesEnabled)
            marker.setMap(this.getMap());
        
        return marker;
    },
    
    clearSiteMarkers : function() {
        Ext.each(this.sites, function(mrk){
            mrk.setMap(null);
        });
    },
    
    showSiteMarkers : function() {
        this.sitesEnabled = true;
        this.loadViewportSites();
        Ext.each(this.sites, function(mrk){
            mrk.setMap(Ext.getCmp('map').getMap());
        });
    },
    
    hideSiteMarkers : function() {
        this.sitesEnabled = false;
        Ext.each(this.sites, function(mrk){
            mrk.setMap(null);
        });
    },
    
    createSiteBubble : function(marker, bubbleContent){ 
        var infoBubble = new InfoBubble({
            map: this.map,
            id: marker.id,
            marker: marker,
            content: bubbleContent.content,
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
            maxHeight: 100,
            wasClicked: false
        });

        var map = this.getMap();
        google.maps.event.addListener(marker, 'mouseover', function() {
            if (!infoBubble.isOpen()) {
                infoBubble.open(map, marker);
            }
        });
        google.maps.event.addListener(marker, 'mouseout', function() {
            if (infoBubble.isOpen() && !infoBubble.wasClicked) {
                infoBubble.close();
            }
        });

        this.siteBubbles.push(infoBubble);

        return infoBubble;
    }
});


