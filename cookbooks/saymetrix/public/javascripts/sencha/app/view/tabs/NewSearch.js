Ext.define('SF.view.tabs.NewSearch', {
    extend: 'Ext.form.Panel',
    alias: 'widget.newsearch',

    mixins: {
        searchtab: 'SF.mixin.SearchTab'
    },

    id: 'newSearch',
    
    config : {
        isValid : true
    },

    constructor : function(config) {
        this.initConfig(config);
        this.addEvents('searchdata','filterclick', 'deepfilter');
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
                Ext.getCmp('newSearch').submitSearch();
                Ext.getCmp('saveNew').enable();
            }
        }, {
            xtype: 'tbseparator'
        }, {
            xtype: 'button',
            id: 'saveNew',
            margin: '0 0 0 4',
            text: 'Save',
            tooltip: 'Save filter',
            disabled: true,
            handler: function(event, toolEl, panel) {
                Ext.getCmp('newSearch').onClickSave();
            }
        }, {
            xtype: 'button',
            id: 'exportNew',
            text: 'Export',
            margin: '0 0 0 4',
            tooltip: 'Export incidents',
            handler: function(){
                Ext.getCmp('newSearch').validateSearch();
                if (Ext.getCmp('newSearch').getIsValid()) {
                    var filterAsJson = Ext.getCmp('newSearch').getFilterAsJson();
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
            id: 'resultsCount',
            flex:1,
            style: {textAlign:'right'},
            text: '0 items'
        }
        ]
    }],


    items: [{
        xtype: 'container',
        id: 'searchContainer',
        layout: 'fit'
    }],
    
    listeners: {
        'activate': {
            scope: this,
            fn: function() {
                if (deepFilterId) { //deepFilterId is declared in index.html as part of the deeplinking for filters
                    Ext.getCmp('newSearch').fireEvent('deepfilter', deepFilterId);
                    deepFilterId = 0;
                    return;
                }
                var tabPanel = Ext.getCmp('tabs');
                var tab = tabPanel.child('[xtype="editsearch"]');
                if (tab) {
                    if (!tab.isDisabled()) {
                        tab.disable();
                        tabPanel.remove(tab);
                    }
                }
                tabPanel.setActiveTab(0);
                Ext.getCmp('newSearch').submitSearch();
                Ext.getCmp('saveNew').disable();
            }
        },
        'afterrender': {
            scope: this,
            fn: function(){
                var searchEL = Ext.get('searchFormDiv');
                Ext.get('searchContainer').update(searchEL.getHTML());
//                Ext.removeNode(searchEL); //IE 8 + 7 not big fans of this line. 
                //set up date pickers
                $('#searchContainer .d1').datepicker();
                $('#searchContainer .d2').datepicker();
                $('#searchContainer .d3').datepicker({position: 'top'});
                $('#searchContainer .d4').datepicker({position: 'top'});
                $('#searchContainer .error-icon').tooltip().hide();
                
                $('#searchContainer input').keyup(function(e) {
                    if (e.which == 13) {
                        Ext.getCmp('newSearch').submitSearch();
                    }
                });
                $('#searchContainer select').keyup(function(e) {
                    if (e.which == 13) {
                        Ext.getCmp('newSearch').submitSearch();
                    }
                });
                $('#searchContainer .incidentTypes').focus();
                
                Ext.getCmp('newSearch').applyIncidentDateRangeValues();
            }
        }
    },
    
    applyIncidentDateRangeValues: function() {
        if ((!$('#searchContainer .incidentStart').val() || $('#searchContainer .incidentStart').val() == "") 
            && (!$('#searchContainer .incidentEnd').val()|| $('#searchContainer .incidentEnd').val() == "")
            && (fromStr != "" || toStr != "")) {
            var pieces = fromStr.substring(0,10).split("-");
            var fromDate = new Date(pieces[0], pieces[1]-1, pieces[2]);
            
            var startTwoDigitDate = ((fromDate.getDate()+1) >= 10)? (fromDate.getDate()) : '0' + (fromDate.getDate());
            var startTwoDigitMonth = ((fromDate.getMonth()+1) >= 10)? (fromDate.getMonth()+1) : '0' + (fromDate.getMonth()+1);
            $('#searchContainer .incidentStart').val(startTwoDigitDate + "/" + startTwoDigitMonth + "/" + fromDate.getFullYear());
        }
    },
    
    updateCount: function(count) {
        Ext.getCmp('resultsCount').update(count + ' items');
        Ext.getCmp('exportNew').setDisabled(count == 0);
    },
    
    submitSearch: function() {
        
        Ext.getCmp('newSearch').validateSearch();
        if (Ext.getCmp('newSearch').getIsValid()) {
            var loadObject = {
                scope:this,
                callback: function(records, operation, success) {
                }
            };
            Ext.getCmp('newSearch').fireEvent('searchdata', loadObject);
        } else {
            Ext.getCmp('saveNew').disable();
        }
    },
    
    validateSearch: function() {
        Ext.getCmp('newSearch').isValid = true;
        $('.error-icon').hide();
		
        //validate the dates
        Ext.getCmp('newSearch').validateDates('#searchContainer .incidentStart','#searchContainer .incidentEnd', false);
        Ext.getCmp('newSearch').validateDates('#searchContainer .blockoutStart','#searchContainer .blockoutEnd', true);
    },
    
    getFilterAsJson: function() {
        var filter = Ext.getCmp('newSearch').returnFilterAsJson('#searchContainer');
        return filter;
    },

    onClickSave: function() {
        Ext.MessageBox.prompt(
            'Name',
            'Please enter a name for the filter:',
            function(btn, text, configs) {
                if(btn == "ok" && Ext.isEmpty(text)) {
                    Ext.MessageBox.prompt(Ext.apply({}, {msg:"Filter name can't be empty"}, configs));
                } else if (btn == "ok") {
                    //remove anything thats not a letter, number, or space from the name - safety first
                    var nameRegex = /[^A-z 0-9]/gm;
                    var filterName = text.replace(nameRegex, '');
                    Ext.MessageBox.show({
                        msg: 'Saving filter, please wait...',
                        progressText: 'Saving...',
                        width:300,
                        wait:true,
                        waitConfig: {
                            interval:200
                        }
                    });
                    var filterAsJson = Ext.getCmp('newSearch').getFilterAsJson();
                    filterAsJson["name"] = filterName;
                    Ext.Ajax.request({
                        url: rootFolder+'/api/filters/personal',
                        jsonData: filterAsJson,
                        method: 'POST',
                        success: function(response) {
                            var jsonResponse = Ext.decode(response.responseText)
                            Ext.MessageBox.hide();
                            Ext.MessageBox.alert('Success', 'Filter saved successfully.');
                            //bring you to edit tab
                            Ext.getCmp('newSearch').fireEvent('filterclick', jsonResponse.id);
                            //disable the button again
                            Ext.getCmp('saveNew').disable();
                        },
                        failure: function(response, opts) {
                            //alert message will eventually come from the response.
                            var errorResponse = Ext.decode(response.responseText)
                            Ext.MessageBox.hide();
                            if (!errorResponse) {
                                Ext.MessageBox.alert('Failure', 'An error occured while attempting to save the filter.<br/>Please try again.');
                            } else {
                                Ext.MessageBox.alert(errorResponse.heading, errorResponse.message);
                            }
                        }
                    });
                } else {
                    Ext.MessageBox.hide();
                }
            }
        );
    }
});
