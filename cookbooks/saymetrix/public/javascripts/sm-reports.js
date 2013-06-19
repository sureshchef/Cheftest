google.load("visualization", "1", {packages:["corechart"]});
google.setOnLoadCallback(prepCharts);

var toDate = getDateObject(toStr),
fromDate = getDateObject(fromStr),
originalToDate = toDate,
originalFromDate = fromDate;

//event handling for buttons etc.
//also sets up the dates and fills in data based on url
$(document).ready(function() {
    if (getParameterByName("d")) {
        $('.dimension.active').removeClass('active');
        $('.dimension a[href="'+getParameterByName("d")+'"]').parent().addClass('active');
    }
    //populate the accounts dropdown
    $.ajax({
        type:"GET",
        url: rootFolder+"/api/accounts/accounts.json"
    }).done(function(accounts){
        $.each(accounts, function(index,account){
            $('#account').append('<option value="'+account.key+'">'+account.name+'</option>');
        });
        if(accounts.length == 1) {
            $('#account').select2();
        } else {
            $('#account').prepend('<option></option');
            $('#account').select2({
                placeholder: 'All Accounts',
                allowClear: true,
                dropdownCssClass: 'report-accounts'
            });
        }
        if (getParameterByName("a") && accountKey == "") {
            accountKey = getParameterByName("a");
        }
        $('#account').select2("val", accountKey);
        $("#account").bind("change", function(){
            submitSearch('');
        });
    });

    $(document).on('click', '.granularity', (function() {
        $('.granularity.active').removeClass('active');
        $(this).addClass('active');
        submitSearch('');
    }));

    $(document).on('click', '.dimension', (function() {
        $('.dimension.active').removeClass('active');
        $(this).addClass('active');
        submitSearch('#dimension-table');
    }));
    
    $(document).on('click', '.dimension a', (function(e) {
        e.preventDefault();
    }));
    
    $('#dimension-table th.c1').text($('.dimension.active').text());
    
    createDatePicker();
    
    $('.print-btn').click(function(e){
        e.preventDefault();
        printPage();
    });
    $('.download-btn').click(function(e){
        e.preventDefault();
        download();
    });

});

function printPage() {
    var urlString = "reports/print";
    urlString += "?s="+getDateString(fromDate);
    urlString += "&e="+getDateString(toDate);
    if ($('#account').select2('val') != null) {
        urlString += "&a="+$('#account').select2('val');
    }
    urlString += "&g="+$('.granularity.active').val();
    
    var preview = window.open(urlString, '_blank', 'height=500,menubar=yes,resizable=no,status=yes,toolbar=yes,width=980');
    var t=setTimeout(function(){preview.print();},1000);
}

function download() {
    var urlString = "reports/download";
    urlString += "?s="+getDateString(fromDate);
    urlString += "&e="+getDateString(toDate);
    if ($('#account').select2('val') != null) {
        urlString += "&a="+$('#account').select2('val');
    }
    urlString += "&g="+$('.granularity.active').val();
    window.open(urlString);
}

function submitSearch(pageLocation) {
    var urlString = "?s="+getDateString(fromDate);
    urlString += "&e="+getDateString(toDate);
    if ($('#account').select2('val') != null) {
        urlString += "&a="+$('#account').select2('val');
    }
    urlString += "&d="+$('.dimension.active a').attr('href');
    urlString += "&g="+$('.granularity.active').val();
    urlString += pageLocation;
    
    //submit the url
    window.location.href= urlString;
}


/* Start of Date Picker functionality */

/* initialises the DatePicker */
function createDatePicker() {
    /* Special date widget */   
    // makes use of fromDate and toDate global vars in index.html
    $('#datepicker-calendar').DatePicker({
        inline: true,
        date: [fromDate, toDate],
        calendars: 3,
        mode: 'range',
        current: new Date(toDate.getFullYear(), toDate.getMonth() - 1, 1),
        onChange: function(dates,el) {
            // update the range display
            fromDate = dates[0];
            toDate = dates[1];
//            $('#date-range-field input').val(dates[0].getDate()+' '+dates[0].getMonthName(true)+', '+dates[0].getFullYear()+' - '+
//                dates[1].getDate()+' '+dates[1].getMonthName(true)+', '+dates[1].getFullYear());
            
            $('#cancelFilter').removeClass('disabled');
            
            if (!$('#fromD').is(':focus') && !$('#toD').is(':focus')) {
                $('#fromD').val(dates[0].getDate()+'/'+(dates[0].getMonth()+1)+'/'+dates[0].getFullYear());
                $('#toD').val(dates[1].getDate()+'/'+(dates[1].getMonth()+1)+'/'+dates[1].getFullYear());
            }
        }
    });
        
    // initialize the special date dropdown field
    $('#date-range-field input').val(fromDate.getDate()+' '+fromDate.getMonthName(true)+', '+fromDate.getFullYear()+' - '+
        toDate.getDate()+' '+toDate.getMonthName(true)+', '+toDate.getFullYear());
        
    // bind a click handler to the date display field, which when clicked
    // toggles the date picker calendar, flips the up/down indicator arrow,
    // and keeps the borders looking pretty
    $('#date-range-field').bind('click', function(){
        $('#datepicker-calendar').toggle();
        return false;
    });
        
    // global click handler to hide the widget calendar when it's open, and
    // some other part of the document is clicked.  Note that this works best
    // defined out here rather than built in to the datepicker core because this
    // particular example is actually an 'inline' datepicker which is displayed
    // by an external event, unlike a non-inline datepicker which is automatically
    // displayed/hidden by clicks within/without the datepicker element and datepicker respectively
    $('html').click(function() {
        if($('#datepicker-calendar').is(":visible")) {
            $('#datepicker-calendar').hide();
            $('#date-range-field').css({
                borderBottomLeftRadius:5, 
                borderBottomRightRadius:5
            });
            $('#date-range-field a').css({
                borderBottomRightRadius:5
            });
        }
    });
        
    // stop the click propagation when clicking on the calendar element
    // so that we don't close it
    $('#datepicker-calendar').click(function(event){
        event.stopPropagation();
    });
    
    /* End special page widget */

    /* Add special date picker functions here, like filling out the form automatically etc. etc. */
    /* Be careful with date calculations as IE may have trouble. */
    $('#reportFilter').click(function(){
        submitSearch('');
    });
    
    $('#cancelFilter').click(function(){
        if (!$('#cancelFilter').hasClass('disabled')) {
            $('#datepicker-calendar').DatePickerSetDate([originalFromDate, originalToDate], true);
            $('#cancelFilter').addClass('disabled');
            setDateInputAsValid('#fromD');
            setDateInputAsValid('#toD');
        }
    });
    
    var currentDates = $('#datepicker-calendar').DatePickerGetDate();
    var fromD = currentDates[0][0];
    var toD = currentDates[0][1];
    $('#fromD').val(fromD.getDate()+"/"+(fromD.getMonth()+1)+"/"+fromD.getFullYear());
    $('#toD').val(toD.getDate()+"/"+(toD.getMonth()+1)+"/"+toD.getFullYear());

    $('#fromD').bind('keyup change', function(e){
        if (e.keyCode < 37 || e.keyCode > 40) {
            onInputChange('#fromD');
        }
    });
    $('#toD').bind('keyup change', function(e){
        if (e.keyCode < 37 || e.keyCode > 40) {
            onInputChange('#toD');
        }
    });
    
    $('#datepicker-calendar .preset-dates .label').click(function(){
        onPresetDateSelected($(this));
    });
}

/* used when either input in the datepicker is changed */
function onInputChange(name) {
    $('#cancelFilter').removeClass('disabled');
    var inputValue = $(name).val();
    if (inputValue != '') {
        var validDate = checkIsValidDate(inputValue);
        if (validDate) {
            //is valid date
            //need to check the fromD is before the toD to complete validation
            var fromD = checkIsValidDate($('#fromD').val());
            var toD = checkIsValidDate($('#toD').val());
            if (fromD && toD) {
                if (fromD <= toD) {
                    setDateInputAsValid('#fromD');
                    setDateInputAsValid('#toD');
                    //select the dates
                    $('#datepicker-calendar').DatePickerSetDate([fromD, toD], true);
                } else {
                    setDateInputAsError('#fromD');
                    setDateInputAsError('#toD');
                }
            }
        } else {
            //is not valid
            setDateInputAsError(name);
        }
    } else {
        //blank = error
        setDateInputAsError(name);
    }
}

/* used to set the input field as being valid (remove error css) and reenable "apply" button if all is good */
function setDateInputAsValid(name) {
    $(name).removeClass('error');
    if ((!$('#fromD').hasClass('error')) && (!$('#toD').hasClass('error'))) {
        $('#reportFilter').removeAttr('disabled').removeClass('disabled');
    }
}

/* used to set an input field as having an error and disable "apply" button */
function setDateInputAsError(name) {
    $(name).addClass('error');
    $('#reportFilter').attr('disabled', 'disabled').addClass('disabled');
}

/* used to check if a value is a date, returns the date object, or false if not in the desired format */
function checkIsValidDate(dateValue) {
    //desired format = dd/mm/yyyy
    var theDate = dateValue.split('/');
    if (theDate[1] > 0 && theDate[1] < 13 &&
        theDate[2] && theDate[2].length === 4 && 
        theDate[0] > 0 && theDate[0] <= (new Date(theDate[2], theDate[1], 0)).getDate()
    ) {
        return new Date(theDate[2], theDate[1]-1, theDate[0]);
    } else {
        //not a valid date
        return false;
    }
}

function onPresetDateSelected(target) {
    var newToD = new Date(), newFromD = new Date()
    noOfDays = 0;
    if (target.hasClass('week')) {
        //find the start and end of this week, then minus 7 days
        var date = new Date();
        newFromD = new Date(date.getFullYear(), date.getMonth(), date.getDate() - date.getDay() - 7);
        newToD = new Date(date.getFullYear(), date.getMonth(), date.getDate() - date.getDay() + 6 - 7);
    } else if (target.hasClass('month')) {
        //find the first day of this month, minus an hour, will give us the toD date
        //then the fromD date is the 1st of the toD month
        var date = new Date();
        newToD = new Date(date.getFullYear(), date.getMonth(), 1);
        newToD.setHours(-1);
        newFromD = new Date(newToD.getFullYear(), newToD.getMonth(), 1);
    } else if (target.hasClass('quarter')) {
        //based on todays' date, find out what quarter we are in, then minus 1 for last quarter
        //then work out months based on that
        //toD date will +1 extra month, then -1 day to get proper and date
        var date = new Date(),
        month = date.getMonth(),
        quarter = (Math.floor(month/3))+1,
        lastQuarter = (quarter > 1) ? quarter - 1 : lastQuarter = 4,
        year;
        if (lastQuarter < 4) {
            year = date.getFullYear();
        } else {
            year = date.getFullYear()-1;
        }
        newFromD = new Date(year, ((lastQuarter-1)*3), 1);
        newToD = new Date(year, newFromD.getMonth()+3, 0);
    } else if (target.hasClass('year')) {
        //get today's year, and -1
        //toD date will be start of this year, -1 day
        var date = new Date();
        newFromD = new Date(date.getFullYear()-1, 0, 1);
        newToD = new Date(date.getFullYear(), 0, 0);
    }
    
    $('#fromD').val(newFromD.getDate()+'/'+(newFromD.getMonth()+1)+'/'+newFromD.getFullYear());
    $('#toD').val(newToD.getDate()+'/'+(newToD.getMonth()+1)+'/'+newToD.getFullYear());
    
    //make sure dates are valid before selecting
    $('#datepicker-calendar').DatePickerSetDate([newFromD, newToD], true);
}

/* End of DatePicker functionality */


function prepCharts() {
    //pie chart
    addPieChart();
    
    //main chart
    var account = "";
    if (typeof $('#account').select2('val') === 'string') {
        account = $('#account').select2('val');
    } else {
        account = getParameterByName('accountkey');
        if (account == null) {
            account = accountKey;
        }
    }
    $.ajax({
        url: rootFolder+"/api/stats.json",
        method: "GET",
        dataType: 'json',
        data: {
            's': getDateString(fromDate),
            'e': getDateString(toDate),
            'a': account,
            'g': $('.granularity.active').val()
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
    if (dataIncidents > 0 || voiceIncidents > 0) {
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