/* 
 * Any fixes needed to make things work in IE
 */

//Believe it or not, in IE9 (but not IE8 or lower), using console.log(something) will actually stop the code from executing further unless the developer window is open.
//Because... y'know... why not?
if (!window.console) (function() {

    var __console, Console;

    Console = function() {
        var check = setInterval(function() {
            var f;
            if (window.console && console.log && !console.__buffer) {
                clearInterval(check);
                f = (Function.prototype.bind) ? Function.prototype.bind.call(console.log, console) : console.log;
                for (var i = 0; i < __console.__buffer.length; i++) f.apply(console, __console.__buffer[i]);
            }
        }, 1000); 

        function log() {
            this.__buffer.push(arguments);
        }

        this.log = log;
        this.error = log;
        this.warn = log;
        this.info = log;
        this.__buffer = [];
    };

    __console = window.console = new Console();
})();