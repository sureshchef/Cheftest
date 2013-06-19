Ext.define('SF.view.tabs.EditSearch', {
    extend: 'Ext.form.Panel',
    alias: 'widget.editsearch',

    mixins: {
        searchtab: 'SF.mixin.SearchTab'
    },
    
    id: 'editSearch',
    
    config : {
        isValid : true
    },

    constructor : function(config) {
        this.initConfig(config);
        this.addEvents('searchdata');
        this.callParent([config]);
    },
    
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        padding:'2 0 5 0',
        items: [
        {
            xtype: 'button',
            margin: '0 0 0 4',
            text: '<i class="icon-search"></i> Search',
            tooltip: 'Search incidents',
            handler: function(event, toolEl, panel) {
            	Ext.getCmp('editSearch').submitSearch();
                Ext.getCmp('saveEdit').enable();
            }
        }, {
            xtype: 'tbseparator'
        }, {
            xtype: 'button',
            id: 'saveEdit',
            margin: '0 0 0 4',
            text: 'Save',
            tooltip: 'Save filter',
            disabled: true,
            handler: function(event, toolEl, panel) {
            	Ext.getCmp('editSearch').onClickSave();
            }
        }, {
            xtype: 'button',
            id: 'exportEdit',
            text: 'Export',
            margin: '0 0 0 4',
            tooltip: 'Export incidents',
            handler: function(){
                Ext.getCmp('editSearch').validateSearch();
                if (Ext.getCmp('editSearch').getIsValid()) {
                    var filterAsJson = Ext.getCmp('editSearch').getFilterAsJson();
                    var filterId=Math.floor(Math.random()*1001)
                    Ext.Ajax.request({
                        url: rootFolder+'/api/exportFilter/'+filterId,
                        jsonData: filterAsJson,
                        method: 'POST',
                        success: function(response) {
                            window.location.assign(rootFolder+'/api/exportIncidents/'+filterId);
                        },
                        failure: function(response, opts) {
                            console.log('Server-side failure with status code ' + response.status);
                            Ext.Msg.alert('Failure', 'Server-side failure with status code ' + response.status);
                        }
                    });
                }
            }
        }, {
            xtype: 'tbseparator'
        }, {
        	xtype: 'text',
        	id: 'editResultsCount',
        	flex:1,
            style: {textAlign:'right'},
        	text: '0 items'
        }
        ]
    }],


    items: [{
        xtype: 'container',
        id: 'editSearchContainer',
        layout: 'fit'
    }],
    
    listeners: {
        'afterrender': {
            scope: this,
            fn: function(){
                var searchEL = Ext.get('searchFormDiv');
                Ext.get('editSearchContainer').update(searchEL.getHTML());
//                Ext.removeNode('searchFormDiv'); //IE 8 + 7 not big fans of this line. 
                //set up date pickers
                $('#editSearchContainer .d1').datepicker();
                $('#editSearchContainer .d2').datepicker();
                $('#editSearchContainer .d3').datepicker({position: 'top'});
                $('#editSearchContainer .d4').datepicker({position: 'top'});
                $('#editSearchContainer .error-icon').tooltip().hide();
                
                $('#editSearchContainer input').keyup(function(e) {
                    if (e.which == 13) {
                        Ext.getCmp('editSearch').submitSearch();
                    }
                });
                $('#editSearchContainer select').keyup(function(e) {
                    if (e.which == 13) {
                        Ext.getCmp('editSearch').submitSearch();
                    }
                });
                $('#editSearchContainer .incidentTypes').focus();
            }
        }
    },
    
    updateCount: function(count) {
    	Ext.getCmp('editResultsCount').update(count + ' items');
        Ext.getCmp('exportEdit').setDisabled(count == 0);
    },
    
    submitSearch: function() {
        Ext.getCmp('editSearch').validateSearch();
    	if (Ext.getCmp('editSearch').getIsValid()) {
            var loadObject = {
                scope:this,
                callback: function(records, operation, success) {
                }
            };
            Ext.getCmp('editSearch').fireEvent('searchdata', loadObject);
        }
    },
    
    validateSearch: function() {
        Ext.getCmp('editSearch').isValid = true;
		$('.error-icon').hide();
		
		//validate the dates
        Ext.getCmp('editSearch').validateDates('#editSearchContainer .incidentStart','#editSearchContainer .incidentEnd', false);
        Ext.getCmp('editSearch').validateDates('#editSearchContainer .blockoutStart','#editSearchContainer .blockoutEnd', true);
    },
    
    getFilterAsJson: function() {
        var filter = Ext.getCmp('newSearch').returnFilterAsJson('#editSearchContainer');
        return filter;
    },

    updateWithFilter: function(filter) {
    	if (filter.id) {
    		$('#editSearchContainer .filterId').val(filter.id);
    	}
    	if (filter.name) {
    		$('#editSearchContainer .filterName').val(filter.name);
    	}
        if (filter.incidentPeriodStart) {
            $('#editSearchContainer .incidentStart').val(filter.incidentPeriodStart);
            $('#editSearchContainer .d1').datepicker('setValue', filter.incidentPeriodStart);
        }
        if (filter.incidentPeriodEnd) {
            $('#editSearchContainer .incidentEnd').val(filter.incidentPeriodEnd);
            $('#editSearchContainer .d2').datepicker('setValue', filter.incidentPeriodEnd);
        }
    	if (filter.accounts && filter.accounts.length > 0) {
    		var accounts = [];
    		for(var i = 0; i < filter.accounts.length; i++) {
				accounts.push(filter.accounts[i].key);
    		};
    		$('#editSearchContainer .accounts').val(accounts);
    	}
    	if (filter.cellID) {
    		$('#editSearchContainer .cellID').val(filter.cellID);
    	}
    	if (filter.msisdn) {
    		$('#editSearchContainer .msisdn').val(filter.msisdn);
    	}
    	if (filter.incidentTypes && filter.incidentTypes.length > 0) {
    		var incidentTypes = [];
            if (typeof filter.incidentTypes[0] === 'string') {
                incidentTypes = filter.incidentTypes;
            } else {
                for(var i = 0; i < filter.incidentTypes.length; i++) {
                    incidentTypes.push(filter.incidentTypes[i].key);
                }
            }
    		$('#editSearchContainer .incidentTypes').val(incidentTypes);
    	}
    	if (filter.locationTech && filter.locationTech.length > 0) {
    		$('#editSearchContainer .locationTech').val(filter.locationTech);
    	}
    	if (filter.position && filter.position.length > 0) {
    		$('#editSearchContainer .position').val(filter.position);
    	}
    	if (filter.frequency && filter.frequency.length > 0) {
    		$('#editSearchContainer .frequency').val(filter.frequency);
    	}
    	if (filter.blockoutPeriod && filter.blockoutPeriod.length > 0) {
    		$('#editSearchContainer .blockoutStart').val(filter.blockoutPeriod[0]);
    		$('#editSearchContainer .blockoutEnd').val(filter.blockoutPeriod[1]);
    	}
    	
    	Ext.getCmp('editSearch').submitSearch();
    },
	
    onClickSave: function() {
        Ext.getCmp('editSearch').validateSearch();
    	if (Ext.getCmp('editSearch').getIsValid()) {
            Ext.MessageBox.show({
                msg: 'Saving filter, please wait...',
                progressText: 'Saving...',
                width:300,
                wait:true,
                waitConfig: {interval:200}
            });
            var filterAsJson = Ext.getCmp('editSearch').getFilterAsJson();
            filterAsJson["name"] = $('#editSearchContainer .filterName').val();
            var filterId = $('#editSearchContainer .filterId').val();
            Ext.Ajax.request({
                url: rootFolder+'/api/filters/'+filterId,
                jsonData: filterAsJson,
                method: 'PUT',
                success: function(response) {
                    //some kind of success msg
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert('Success', 'Filter saved successfully.');
                },
                failure: function(response, opts) {
                    //message should come from response
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert('Failure', 'An error occured while attempting to save the filter.<br/>Please try again.');
                }
            });
        }
    }
});