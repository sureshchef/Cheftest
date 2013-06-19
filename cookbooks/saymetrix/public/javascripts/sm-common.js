function toggleUI(selector, enable) {
    if (enable) {
        //re-enable/disable UI elements
        $('#create-'+selector+' .modal-header .spinner').hide();
        $('#'+selector+'-create').removeClass('disabled');
        $('#'+selector+'-back').removeClass('disabled');
        $('#create-'+selector+' .modal-footer button:first').removeAttr('disabled');
        $('#create-'+selector+'').data('modal').isShown = true;
    } else {
        $('#create-'+selector+' .modal-header .spinner').show();
        $('#'+selector+'-create').addClass('disabled');
        $('#'+selector+'-back').addClass('disabled');
        $('#create-'+selector+' .modal-footer button:first').attr('disabled', true);
        $('#create-'+selector+'').data('modal').isShown = false;
    }
}

function addErrorToInput(identifier, error) {
    if (error) {
        $(identifier).closest('.control-group').addClass("error");
        $(identifier).siblings('.help-inline').html(error);
    }
}

//needed to help format dates properly in older browsers
if (!Date.prototype.toISOString) {
    (function() {
        function pad(number) {
            var r = String(number);
            if ( r.length === 1 ) {
                r = '0' + r;
            }
            return r;
        }

        Date.prototype.toISOString = function() {
            return this.getUTCFullYear()
            + '-' + pad( this.getUTCMonth() + 1 )
            + '-' + pad( this.getUTCDate() )
            + 'T' + pad( this.getUTCHours() )
            + ':' + pad( this.getUTCMinutes() )
            + ':' + pad( this.getUTCSeconds() )
            + '.' + String( (this.getUTCMilliseconds()/1000).toFixed(3) ).slice( 2, 5 )
            + 'Z';
        };
    }() );
}
//needed to help get dates from UTC strings properly in older browsers
function fromISOString(string) {
    var isoDateExpression = /^(\d{4}|[+\-]\d{6})(?:\-(\d{2})(?:\-(\d{2})(?:T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{3}))?)?(Z|(?:([\-\+])(\d{2}):(\d{2})))?)?)?)?$/,
        monthes = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365];
    function dayFromMonth(year, month) {
        var t = month > 1 ? 1 : 0;
        return monthes[month] + Math.floor((year - 1969 + t) / 4) - Math.floor((year - 1901 + t) / 100) + Math.floor((year - 1601 + t) / 400) + 365 * (year - 1970);
    }
    var match = isoDateExpression.exec(string);
    if (match) {
    // parse months, days, hours, minutes, seconds, and milliseconds
    // provide default values if necessary
    // parse the UTC offset component
    var year = Number(match[1]),
        month = Number(match[2] || 1) - 1,
        day = Number(match[3] || 1) - 1,
        hour = Number(match[4] || 0),
        minute = Number(match[5] || 0),
        second = Number(match[6] || 0),
        millisecond = Number(match[7] || 0),
        // When time zone is missed, local offset should be used (ES 5.1 bug)
        // see https://bugs.ecmascript.org/show_bug.cgi?id=112
        offset = !match[4] || match[8] ? 0 : Number(new Date(1970, 0)),
        signOffset = match[9] === "-" ? 1 : -1,
        hourOffset = Number(match[10] || 0),
        minuteOffset = Number(match[11] || 0),
        result;
    if (hour < (minute > 0 || second > 0 || millisecond > 0 ? 24 : 25) && 
        minute < 60 && second < 60 && millisecond < 1000 && 
        month > -1 && month < 12 && hourOffset < 24 && minuteOffset < 60 && // detect invalid offsets
        day > -1 && day < dayFromMonth(year, month + 1) - dayFromMonth(year, month)) {
        result = ((dayFromMonth(year, month) + day) * 24 + hour + hourOffset * signOffset) * 60;
        result = ((result + minute + minuteOffset * signOffset) * 60 + second) * 1000 + millisecond + offset;
        if (-8.64e15 <= result && result <= 8.64e15) {
            return result;
        }
    }
    }
    return NaN;
};
