Ext.application({
    name: 'SF',
    
    appFolder: rootFolder+'/public/javascripts/sencha/app',

    controllers: ['Incidents', 'Tabs'],
 
    launch: function() {
        Ext.create('Ext.container.Viewport', {
            layout: 'border',
            autoCreateViewPort:false,
            itemId:'mapView',
            items: [{
                region:'west',
                style:{
                    'padding-top':'40px'
                },
                xtype: 'searchtabs',
                collapsible: true,
                header: false,
                title: 'Incident Navigator',
                split: true,
                minWidth: 270,
                maxWidth: 400,
                width: 270
            }, {
            	region: 'center',
            	layout: 'border',
                style:{
                    'padding-top':'40px'
                },
            	items: [ {
        	    	region: 'south',
                    id: 'bottomBar',
                    split: true,
                    height: 250,
                    minHeight: 150,
                    maxHeight: 400,
                    layout: 'border',
                    items: [{
                        region: 'east',
                        split:true,
                        minWidth: 235,
                        maxWidth: 400,
                        width: 300,
                        id: 'details',
                        title: 'Incident Detail',
                        xtype: 'incidentdetail'
                    }, {
                        region: 'center',
                        id: 'list',
                        xtype: 'incidentlist',
                        emptyText: 'No matching incidents found.'
                    }]
        	    }, {
        	    	region: 'center',
                    xtype: 'incidentmap',
                    zoomLevel: 12,
                    gmapType: 'map',
                    id: 'map',
                    mapConfOpts: ['enableScrollWheelZoom','enableDoubleClickZoom','enableDragging'],
                    setCenter: {
                        lat: latitude,
                        lng: longitude
                    }
        	    }]   
            }]
        });
    }
});
