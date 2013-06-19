Ext.define('SF.view.incident.Detail', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.incidentdetail',

    html: '<div id="holder"><h3><center>No Incident Selected</center></h3></div>',
    autoScroll: true,

    update:function(data) {
    	var appIncidentTpl = new Ext.XTemplate(
    	        '<div id="incident-detail" style="padding-top:6px;">',
    				'<div style="height:26px;">',
                        '<button class="btn btn-mini pull-right" style="margin-left:6px;" onclick="associateIncidentWithEvent({id})" title="Associate with network event"><i class="icon-retweet"></i></button>',
        			'</div>',
    				'<div>',
                        '<div style="float:left; margin-bottom:5px;">',
                            '<img src="'+rootFolder+'/public/images/map-icons/markers/{incidentGroup}.png" width="50" height="50" />',
                        '</div>',
                        '<div>',
                            '<h4>{incidentType} <small>- {incidentGroup}</small></h4>',
                            '<h6>{dateFormatted} - {timeFormatted}</h6>',
                        '</div>',
                        '<table class="table table-striped table-bordered table-condensed">',
                            '<tr>',
                            '	<td>Subscriber&nbsp;</td><td>{subscriber.firstname} {subscriber.lastname}</td>',
                            '</tr>',
                            '<tr>',
                            '	<td>Account&nbsp;</td><td>{subscriber.account.name}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>MSISDN&nbsp;</td><td>{subscriber.msisdn}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Phone Type&nbsp;</td><td>{phoneType}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Phone OS&nbsp;</td><td>{phoneOs}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Cell ID&nbsp;</td><td>{cellId}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Frequency&nbsp;</td><td>{frequency}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Location Tech&nbsp;</td><td>{locationTech}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Position&nbsp;</td><td>{position}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Address&nbsp;</td><td>{address}</td>',
                            '<tr>',
                                '<td colspan="2">Comment<br/>{comment}</td>',
                            '</tr>',
                        '</table>',
                    '</div>',
		        '</div>'
        );
        var webIncidentTpl = new Ext.XTemplate(
    	        '<div id="incident-detail" style="padding-top:6px;">',
    				'<div style="height:26px;">',
                        '<button class="btn btn-mini pull-right" style="margin-left:6px;" onclick="associateIncidentWithEvent({id})" title="Associate with network event"><i class="icon-retweet"></i></button>',
                        '<button class="btn btn-mini pull-right" onclick="editIncident({id})">Edit</button>',
        			'</div>',
    				'<div>',
                        '<div style="float:left; margin-bottom:5px;">',
                            '<img src="'+rootFolder+'/public/images/map-icons/markers/{incidentGroup}.png" width="50" height="50" />',
                        '</div>',
                        '<div>',
                            '<h4>{incidentType} <small>- {incidentGroup}</small></h4>',
                            '<h6 class="pull-left">{dateFormatted} - {timeFormatted}</h6>',
                        '</div>',
                        '<table class="table table-striped table-bordered table-condensed">',
                            '<tr>',
                            '	<td>Subscriber&nbsp;</td><td>{subscriber.firstname} {subscriber.lastname}</td>',
                            '</tr>',
                            '<tr>',
                            '	<td>Account&nbsp;</td><td>{subscriber.account.name}</td>',
                            '</tr>',
                            '<tr>',
                            '	<td>Reported By&nbsp;</td><td>{reporter.firstname} {reporter.lastname}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>MSISDN&nbsp;</td><td>{subscriber.msisdn}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Phone Type&nbsp;</td><td>{phoneType}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Phone OS&nbsp;</td><td>{phoneOs}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Frequency&nbsp;</td><td>{frequency}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Location Tech&nbsp;</td><td>{locationTech}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Position&nbsp;</td><td>{position}</td>',
                            '</tr>',
                            '<tr>',
                                '<td>Address&nbsp;</td><td>{address}</td>',
                            '<tr>',
                            '<tr>',
                                '<td colspan="2">Comment<br/>{comment}</td>',
                            '</tr>',
                        '</table>',
                    '</div>',
                '</div>'
        );
    	var tplData = {
    		dateFormatted: Ext.Date.format(data.date, "d/m/Y"),
    		timeFormatted: Ext.Date.format(data.date, "H:i:s")
    	};
    	data = Ext.Object.merge(data, tplData);
        var that = this;
    	if(data.incidentGroup != undefined) {
            if (data.source == 2){
                webIncidentTpl.overwrite(that.body, data);
            } else {
                appIncidentTpl.overwrite(that.body, data);
            }
    	} else {
    		this.body.update('<div id="holder"><h3><center>No Incident Selected</center></h3></div>');
    	}
    }
});