Ext.define('SF.store.MapSites', {
    extend: 'Ext.data.Store',
    model: 'SF.model.Site',

    proxy: {
    	type: 'ajax',
    	api: {
    		read: ''
    	},
    	reader: {
    		type: 'json',
    		root: 'sites',
    		successProperty: 'success'
    	}
    }
});