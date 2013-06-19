Ext.define('SF.store.ListIncidents', {
    extend: 'Ext.data.Store',
    model: 'SF.model.Incident',
    // TODO page size should really be 100, but extjs for some reason loves the number 35 (only 35 rows are actually loaded)
    pageSize: 35,
    remoteSort: true,
    
    buffered: true,

    proxy: {
    	type: 'ajax',
    	url: rootFolder+'/api/paged-incidents.json',
        actionMethods: {
            read: 'POST'
        },
    	reader: {
    		type: 'json',
    		root: 'incidents',
            totalProperty: 'totalCount',
    		successProperty: 'success'
    	},
        simpleSortMode: true,
        /*
        * override Ext Ajax Proxy doRequest method
        * must be maintained when Ext library is updated in the app
        */
        doRequest: function(operation, callback, scope) {
            var writer  = this.getWriter(),
                request = this.buildRequest(operation, callback, scope);

            if (operation.allowWrite()) {
                request = writer.write(request);
            }

            Ext.apply(request, {
                headers       : this.headers,
                timeout       : this.timeout,
                scope         : this,
                callback      : this.createRequestCallback(request, operation, callback, scope),
                method        : this.getMethod(request),
                disableCaching: false // explicitly set it to false, ServerProxy handles caching
            });

            /*
            * do anything needed with the request object
            */
            var tab = Ext.getCmp('tabs').getActiveTab();
            if (tab.xtype == "filtertab") {
                tab = Ext.getCmp('newSearch');
            }
            request.jsonData = tab.getFilterAsJson();
           
//            console.log('request', request);
//            console.log('request.params', request.params);

            Ext.Ajax.request(request);

            return request;
        }
    },
    sorters: [{
        property: 'date',
        direction: 'DESC'
    }]
});