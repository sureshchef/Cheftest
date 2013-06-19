Ext.define('SF.view.tabs.Filter', {
    extend: 'Ext.form.Panel',
    alias: 'widget.filtertab',

    id: 'filterTab',
    autoScroll: true,

    bodyPadding: '10px 0',
 
    initComponent: function() {
        this.addEvents('filterclick');
        this.callParent(arguments);
    },
    
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        padding:'2 0 5 0',
        items: [
        {
            xtype: 'button',
            margin: '0 0 0 4',
            text: '<i class="icon-refresh"></i> Refresh',
            tooltip: 'Refresh Filters',
            handler: function(event, toolEl, panel) {
                Ext.getCmp('filterTab').refreshFilterList();
            }
        }
        ]
    }],

    items: [
        {
        	xtype: 'container',
        	id: 'filterContainer',
        	layout: 'fit'
        }
    ],
    listeners: {
        'activate': {
            scope: this,
            fn: function() {
                var tabPanel = Ext.getCmp('tabs');
                var tab = tabPanel.child('[xtype="editsearch"]');
                if (tab) {
                    if (!tab.isDisabled()) {
                        tab.disable();
                        tabPanel.remove(tab);
                    }
                }
                tabPanel.setActiveTab(1);
                Ext.getCmp('filterTab').refreshFilterList();
            }
        },
        'afterrender': {
            scope: this,
            fn: function(){
                var filterEL = Ext.get('filterListDiv');
                Ext.get('filterContainer').update(filterEL.getHTML());
//                Ext.removeNode('filterListDiv'); //IE 8 + 7 not big fans of this line. 
                Ext.getCmp('filterTab').applyJQueryHandlers();
            }
        }
    },
    
    applyJQueryHandlers: function() {
        //jquery handler for clicking a filter
        $('#filterContainer a.filter').click(function(e){
        	e.preventDefault();
        	Ext.getCmp('filterTab').onFilterClick($(this).attr('href').substring(1));
        });

        //jquery handler for clicking rename for a filter
        $('#filterContainer a.rename').click(function(e){
        	e.preventDefault();
        	var filterId = $(this).parent().parent().children().first().children().first().attr('href').substring(1);
        	Ext.getCmp('filterTab').onRenameClick($(this).attr('href').substring(1), filterId);
        });

        //jquery handler for clicking delete for a filter
        $('#filterContainer a.delete').click(function(e){
        	e.preventDefault();
        	Ext.getCmp('filterTab').onDeleteClick($(this).attr('href').substring(1));
        });
    },

    onFilterClick: function(filterId) {
    	this.fireEvent('filterclick', filterId);
    },

    onRenameClick: function(filterName, filterId) {
    	Ext.MessageBox.prompt(
			'Name',
			'Please enter a name for the filter:',
			function(btn, text, configs) {
				if(btn == "ok" && (Ext.isEmpty(text)||filterName == text)) {
                    Ext.MessageBox.prompt(Ext.apply({}, {msg:"Filter name was not changed"}, configs));
                } else if(btn == "ok") {
                    //remove anything thats not a letter, number, or space from the name - safety first
                    var nameRegex = /[^A-z 0-9]/gm;
                    var newFilterName = text.replace(nameRegex, '');
                    
			    	Ext.MessageBox.show({
			            msg: 'Saving filter, please wait...',
			            progressText: 'Saving...',
			            width:300,
			            wait:true,
			            waitConfig: {interval:200}
			        });
	
                    //get the filter
                    Ext.Ajax.request({
                        url: rootFolder+'/api/filters/'+filterId,
                        method: 'GET',
                        success: function(response) {
                            var filterAsJson = Ext.decode(response.responseText);
                            filterAsJson["name"] = newFilterName;

                            if (filterAsJson.accounts && filterAsJson.accounts.length > 0) {
                                var accounts = [];
                                for(var i = 0; i < filterAsJson.accounts.length; i++) {
                                    accounts.push(filterAsJson.accounts[i].key);
                                }
                                filterAsJson["accounts"] = accounts;
                            }

                            if (filterAsJson.incidentPeriod) {
                                if(filterAsJson.incidentPeriod.length > 0) {
                                    filterAsJson.incidentPeriod = filterAsJson.incidentPeriod.split(",");
                                }
                            }

                            if (filterAsJson.incidentTypes && filterAsJson.incidentTypes.length > 0) {
                                var incidentTypes = [];
                                for(var i = 0; i < filterAsJson.incidentTypes.length; i++) {
                                    incidentTypes.push(filterAsJson.incidentTypes[i].key);
                                }
                                filterAsJson["incidentTypes"] = incidentTypes;
                            }

                            Ext.Ajax.request({
                                url: rootFolder+'/api/filters/'+filterId,
                                jsonData: filterAsJson,
                                method: 'PUT',
                                success: function(response) {
                                    //some kind of success msg
                                    Ext.MessageBox.hide();
                                    Ext.getCmp('filterTab').refreshFilterList();
                                    Ext.MessageBox.alert('Success', 'Filter saved successfully.');
                                },
                                failure: function(response, opts) {
                                    //message should come from response
                                    Ext.MessageBox.hide();
                                    Ext.MessageBox.alert('Failure', 'An error occured while attempting to save the filter.<br/>Please try again.');
                                }
                            });
                        },
                        failure: function(response, opts) {
                            //should probably get message from response
                            Ext.MessageBox.hide();
                            Ext.MessageBox.alert('Failure', 'An error occured while attempting to save the filter.<br/>Please try again.');
                        }
                    });
				} else {
    				Ext.MessageBox.hide();
    				return;
    			}
		    },
			'window',
			false,
			filterName
    	);
    },

    onDeleteClick: function(filterId) {
    	Ext.MessageBox.confirm(
    		'Confirm Delete',
    		'Are you sure you want to delete this filter?<br/>This procedure is irreversible.',
    		function(btn, text) {
    			if (btn == "yes") {
			    	Ext.MessageBox.show({
			            msg: 'Deleting filter, please wait...',
			            progressText: 'Deleting...',
			            width:300,
			            wait:true,
			            waitConfig: {interval:200}
			        });
	
			    	Ext.Ajax.request({
			            url: rootFolder+'/api/filters/'+filterId,
			            method: 'DELETE',
			            success: function(response) {
			            	//some kind of success msg
			                Ext.MessageBox.hide();
			                Ext.getCmp('filterTab').refreshFilterList();
			            },
			            failure: function(response, opts) {
			            	//message should come from response
			            	Ext.MessageBox.hide();
			            	Ext.MessageBox.alert('Failure', 'An error occured while attempting to delete the filter.<br/>Please try again.');
			            }
			        });
    			} else {
    				Ext.MessageBox.hide();
    				return;
    			}
		    }
    	);
    },
    
    refreshFilterList: function() {
    	Ext.Ajax.request({
            url: rootFolder+'/api/filters.json',
            method: 'GET',
            success: function(response) {
            	var filterList = Ext.decode(response.responseText);
                $('#filterContainer thead tr').remove();
                $('#filterContainer tbody tr').remove();
                if (filterList.length > 0) {
                    var htmlString = "<tr><th>Name</th><th class='actions'>Actions</th></tr>";
                    $('#filterContainer thead').append(htmlString);
                    $.each(filterList, function(index, filter){
                        htmlString = 
                            "<tr>" +
                                "<td class='filterName'><a class='filter' href='#"+filter.id+"' title='"+filter.name+"'>"+filter.name+"</a></td>" +
                                "<td class='actions'>" +
                                    "<a class='rename btn btn-mini' href='#"+filter.name+"' title='rename filter'>rename</a> " +
                                    "<a class='delete btn btn-mini btn-danger' href='#"+filter.id+"' title='delete filter'>delete</a>" +
                                "</td>" +
                            "</tr>";
                        $('#filterContainer tbody').append(htmlString);
                    });
                    Ext.getCmp('filterTab').applyJQueryHandlers();
                } else {
                    var htmlString = 
                        "<tr>" +
                            "<th>No filters available at this time</th>"
                        "</tr>";
                    $('#filterContainer thead').append(htmlString);
                }
            },
            failure: function(response, opts) {
            	console.log(response);
            	//should an alert go here?
            }
        });
    }
});