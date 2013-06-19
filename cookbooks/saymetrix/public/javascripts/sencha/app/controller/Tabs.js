Ext.define('SF.controller.Tabs', {
    extend: 'Ext.app.Controller',

    views: [
    	'tabs.SearchTabs', 'tabs.NewSearch', 'tabs.Filter', 'tabs.EditSearch'
    ],
 
    modifyFilter : false,
    
    init: function() {
        this.control({
            'filtertab': {
                filterclick: this.onFilterClick
            },
            'newsearch': {
                filterclick: this.onFilterClick,
                deepfilter: this.onDeepFilter
            }
        });
    },
    
    onFilterClick: function(filterId) {
        var me = this;
    	Ext.Ajax.request({
            url: rootFolder+'/api/filters/'+filterId,
            method: 'GET',
            success: function(response) {
                var filter = Ext.decode(response.responseText);
                me.getController('Tabs').openFilter(filter);
            },
            failure: function(response, opts) {
            	console.log('Server-side failure with status code ' + response.status);
                if (response.status == 404) {
                    Ext.Msg.alert('Not Found', 'The requested filter could not be found.');
                    me.getController('Tabs').modifyFilter = false;
                    //this will cause the grid to show its empty message
                    Ext.getStore('ListIncidents').loadData({});
                } else {
                    Ext.Msg.alert('Failure', 'Server-side failure with status code ' + response.status);
                }
            }
        });
    },
    
    onDeepFilter: function(filterId) {
        this.modifyFilter = true;
        this.onFilterClick(filterId);
    },
    
    openFilter: function(filter) {
        if (this.modifyFilter) {
            this.modifyFilter = false;
            if (useFilterFromDate) {    //this variable is found in index.html
                var dtRegex = /^\d{4}\-\d{1,2}\-\d{1,2}$/;
                if(dtRegex.test(useFilterFromDate)) {
                    var data = useFilterFromDate.split('-'),
                        yyyy = parseInt(data[0],10),
                        mm = parseInt(data[1],10),
                        dd = parseInt(data[2],10),
                        date = new Date(yyyy,mm-1,dd);
                    if ((date.getFullYear() == yyyy) && (date.getMonth() == mm - 1) && (date.getDate() == dd)) {
                        filter.incidentPeriodStart = dd+"/"+mm+"/"+yyyy;
                    }
                }
                useFilterFromDate = false;
            }
        }
        
        var tabPanel = Ext.getCmp('tabs');
        var tab = tabPanel.child('[xtype="editsearch"]');
        if (!tab) {
            tab = tabPanel.add({title:filter.name, tooltop:'Edit Search', xtype: 'editsearch'});
            tabPanel.setActiveTab(tab);
            Ext.getCmp('editSearch').updateWithFilter(filter);
        }
    }
});