Ext.define('SF.view.tabs.SearchTabs', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.searchtabs',

    id: 'tabs',
    
    activeTab: 0,
    
    maxTabWidth: 100,

	items: [
		{
            tabConfig: {
                title:'New',
                tooltop:'New Search'
            },
            xtype: 'newsearch'
        },{
	        tabConfig: {
                title:'Filters',
                tooltop:'Manage Filters'
            },
            xtype: 'filtertab'
	    }
    ],
    
    updateActiveTabResultsCount: function(count) {
        var tab = Ext.getCmp('tabs').getActiveTab();
        if (tab.xtype != "filtertab") {
            tab.updateCount(count);
        }
    }
        
});
