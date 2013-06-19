// Load google chart script
//Ext.require(['Ext.data.*']);
//Ext.require(['Ext.chart.*']);

google.load("visualization", "1", {packages:["corechart"]});
google.setOnLoadCallback(prepCharts);

var toDate = getDateObject(toStr);
var fromDate = getDateObject(fromStr);

function prepCharts() {
    //pie chart
    addPieChart();
    
    //main chart
    var account = "";
    account = getParameterByName('a');
    if (account == null) {
        account = accountKey;
    }
    $.ajax({
        url: rootFolder+"/api/stats.json",
        method: "GET",
        dataType: 'json',
        data: {
            's': getDateString(fromDate),
            'e': getDateString(toDate),
            'a': account,
            'g': granularity
        }
    }).done(function(json) {
        //display graph
        var data = [];
        var maxVal = 4;
        if (json) {
            $.each(json, function(i, item) {
                if (maxVal < item.count) {
                    maxVal = item.count;
                }
                var start, label;
                if (item.date) {
                    start = getDateObject(item.date);
                    label = start.toString("MMMM d, yyyy");
                } else {
                    start = getDateObject(item.start);
                    var end = getDateObject(item.end);
                    label = start.toString("MMMM d, yyyy") + " - " + end.toString("MMMM d, yyyy");
                }
                data.push([{v:start,f:label}, parseInt(item.count, 10)]);
            });
        }
        addAreaChart(data, maxVal)
    }).fail(function(xhr, e, o){
        //display message about graph not found
        $('.graph-placeholder').hide();
        $('.graph-error').show();
    }).always(function(){
        //hide the spinner loader thing
        $('.graph-placeholder').hide();
    });
}

function addAreaChart(json, maxVal) {
        var data = new google.visualization.DataTable();
        data.addColumn('date', 'Date');
        data.addColumn('number', 'Incidents');
        data.addRows(json);

        var options = {
            areaOpacity: 0.1,
            chartArea: {width: '97%', height: '85%'},
            colors: ['#42a3d3','#00cc00'],
            fontSize: 12,
            hAxis: {baselineColor: 'white', format: 'MMM d, y', gridlines: {color:'transparent'}},
            legend: {position: 'top', alignment: 'start'},
            lineWidth: 3,
            pointSize: 5,
            tooltip: {textStyle: {color:'black', fontName:'"Arial"', fontSize: 11}},
            vAxis: {textPosition:'in', maxValue: findMaxAxisValue(maxVal,0,false), minValue: 0}
        };
        var maxDate = getDateObject(toDate.getFullYear() + "-" + (toDate.getMonth()+1) + "-" + toDate.getDate());
        var minDate = getDateObject(fromDate.getFullYear() + "-" + (fromDate.getMonth()+1) + "-" + fromDate.getDate());
        if (maxDate > minDate) {
            options.hAxis["viewWindow"] = {max: maxDate, min: minDate};
        }
        
        var chart = new google.visualization.AreaChart(document.getElementById('main-chart'));
        chart.draw(data, options);
};

function addPieChart() {
    if (dataIncidents > 0 && voiceIncidents > 0) {
        var data = google.visualization.arrayToDataTable([
            ['Type', 'Number'],
            ['Data Incidents', dataIncidents],
            ['Voice Incidents', voiceIncidents],
            ['Other Incidents', otherIncidents]
        ]);

        var options = {
            backgroundColor: 'transparent',
            chartArea: {left:'20%'},
            colors: ['#00cc00','#42a3d3', '#ef791b'],
            fontName:'"Arial"',
            legend:  {position: 'none'},
            pieSliceText: 'none',
            tooltip: {textStyle: {fontSize: 12}}
        };

        var chart = new google.visualization.PieChart(document.getElementById('pie-chart'));
        chart.draw(data, options);
    }
}


/* Utility functions
 ***********************************************/
/* function for getting values from the url */
function getParameterByName(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
}
/* function for finding the maximum value for an axis on a graph */
function findMaxAxisValue(val, min, log) {
    var max = Math.ceil(val);
    if (log) {
        while(Math.pow(max, 0.25) !== parseInt(Math.pow(max, 0.25))) {
            max++;
        }
    } else {
        while ((max - min)/4 !== parseInt((max - min)/4)) {
            max++;
        }
    }
    return max;
}
/* function for getting a date, despite cross-browser issues */
function getDateObject(dateString) {
    var pieces = dateString.substring(0,10).split("-");
    return new Date(pieces[0], pieces[1]-1, pieces[2]);
}
/* function for getting a date as a string in the format YYYY-MM-DD */
function getDateString(date) {
    return date.getFullYear() + "-" + (date.getMonth()+1) + "-" + date.getDate();
}