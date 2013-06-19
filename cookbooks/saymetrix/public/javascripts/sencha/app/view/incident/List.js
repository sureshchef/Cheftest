Ext.define('SF.view.incident.List', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.incidentlist',

    store: 'ListIncidents',
 
    initComponent: function() {
        this.columns = [
        {
            header: 'Id',  
            dataIndex: 'id',  
            flex: 1,
            menuDisabled: true,
            draggable: false,
            hideable: false, 
            hidden: true
        },
        {
        	text: '',
        	xtype:'templatecolumn',
        	tpl:'<div class="grid-icon"><img src="'+rootFolder+'/public/images/grid-icons/{source-img}" alt="{source-name}" title="{source-name}" height="16" /></div>',  
            width: 32,
            menuDisabled: true,
            draggable: false,
            sortable: false,
            resizable: true,
            fixed: true,
            hideable: false, 
            hidden: false
        },
        {
        	text: 'Incident',
            dataIndex: 'incidentName',
            flex: 1,
            menuDisabled: true,
            draggable: false,
            sortable: true,
            hideable: false, 
            hidden: false
        },
        {
        	text: 'Date',
        	dataIndex: 'date',
        	xtype: 'datecolumn',
        	format:'d/m/Y H:i:s',
            flex: 1,
            menuDisabled: true,
            draggable: false,
            sortable: true,
            hideable: false, 
            hidden: false
        },
        {
            header: 'Subscriber', 
            dataIndex: 'subscriberName', 
            flex: 1,
            menuDisabled: true,
            draggable: false,
            sortable: true,
            hideable: false, 
            hidden: false
        },

        {
            header: 'Account', 
            dataIndex: 'accountName', 
            flex: 1,
            menuDisabled: true,
            draggable: false,
            sortable: true,
            hideable: false, 
            hidden: false
        },

        {
            header: 'Location Tech', 
            dataIndex: 'locationTech', 
            flex: 1,
            menuDisabled: true,
            draggable: false,
            hideable: false, 
            hidden: true
        },

        {
            header: 'Latitude', 
            dataIndex: 'latitude', 
            flex: 1,
            menuDisabled: true,
            draggable: false,
            hideable: false, 
            hidden: true
        },

        {
            header: 'Longitude', 
            dataIndex: 'longitude', 
            flex: 1,
            menuDisabled: true,
            draggable: false,
            hideable: false, 
            hidden: true
        }
        ];
 
        this.callParent(arguments);
    }
});