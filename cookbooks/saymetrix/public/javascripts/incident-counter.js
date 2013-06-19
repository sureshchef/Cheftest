var totalIncidents = 0;

var updateIncidents = function() {
    $.getJSON(rootFolder+"/api/totalIncidents.json", function(newIncidents) {
        if (newIncidents != $('#incidents-total strong').text()) {
            if (!isNaN(newIncidents) && newIncidents > 0) {
                totalIncidents = newIncidents;
                countTo($('#incidents-total strong'),addThousandSeparator(parseInt(totalIncidents)));
            } else {
                return false;
            }
        }
    });
}

var addThousandSeparator = function(nStr) {
    nStr += '';
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1;
}

var countTo = function(el, val) {
    if (el.text().length != val.length) {
        el.text(val);
        el.css('width', el.width() + 'px').css('display', 'inline-block');
        return false;
    }
    var digits = el.text().split('');
    el.css('height', '23px').css('width', el.width() + 'px').css('display', 'inline-block');
    el.html("");
    var offset = new Array();
    var digitEles = new Array();
    for (i in digits) {
        var digit = $("<span></span>").text(digits[i]).appendTo(el);
        offset.push(digit.position().left);
        digitEles.push(digit);
    }
    for (i in digitEles) {
        digitEles[i].css({
            top: 0,
            left: offset[i] + "px",
            position: 'absolute'
        })
    }

    var newDigits = val.split('');
    for (i in newDigits) {
        if (newDigits[i] != digits[i]) {
            var newDigit = $('<span></span>').text(newDigits[i]).appendTo(el);
            newDigit.css({
                top: "-10px",
                left: offset[i] + "px",
                position: 'absolute'
            });
            newDigit.animate({
                top: '+=10',
                opacity: 1.0
            }, 200), function() {
                el.html(val)
            };
            digitEles[i].animate({
                top: '+=10',
                opacity: 0.0
            }, 200, function(){
                $(this).remove()
            });
        }
    }

}

$(document).ready(function() {
//    setInterval(updateIncidents, 300000); //every 5 minutes
    updateIncidents();
});
