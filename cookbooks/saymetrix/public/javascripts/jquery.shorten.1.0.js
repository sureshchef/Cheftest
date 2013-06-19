jQuery.fn.shorten = function(settings) {
    var config = {
        showChars : 60,
        ellipsesText : "..."
    };

    if (settings) {
        $.extend(config, settings);
    }

    return this.each(function() {
        var $this = $(this);
        var content = $this.html();
        var p = $this.children('p').text();
        if (p.length > config.showChars) {
            p = p.substr(0, config.showChars) + '<span class="moreellipses">' + config.ellipsesText + '&nbsp;</span>';
        }
        var html = '<p class="shortver">' + p + '</p>' + '<div class="longver">' + content + '</div>';
//        var btn = '<button href="#" class="morelink btn btn-mini pull-right" onclick="morelessclick(this)">more</button>';
        $this.html(html);
//        $this.parent().prepend(btn);
        $(".longver").hide();
    });
};

function morelessclick(el) {
    var $this = $(el);
    if ($this.hasClass('more')) {
        $this.removeClass('more');
        $('.view-event').removeClass('more');
        $('.view-event .longver').hide();
        $('.view-event .shortver').show();
    } else {
        $('.view-event').removeClass('more');
        $('.view-event .longver').hide();
        $('.view-event .shortver').show();
        $this.addClass('more');
        $this.children('.event-details').children('.longver').show();
        $this.children('.event-details').children('.shortver').hide();
    }
//    $this.children('.event-details').children('.longver').toggle();
//    $this.children('.event-details').children('.shortver').toggle();
//    $this.siblings('.event-details').children('.shortver').toggle();
//    $this.siblings('.event-details').children('.longver').toggle();
    return false;
}
