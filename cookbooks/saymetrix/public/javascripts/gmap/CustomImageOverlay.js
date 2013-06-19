var MERCATOR_RANGE = 256;

function bound(f, e, d) {
    if(e != null) {
        f = Math.max(f, e)
    }
    if(d != null) {
        f = Math.min(f, d)
    }
    return f
}
function degreesToRadians(b) {
    return b * (Math.PI / 180)
}
function radiansToDegrees(b) {
    return b / (Math.PI / 180)
}
function MercatorProjection() {
    this.pixelOrigin_ = new google.maps.Point(MERCATOR_RANGE / 2, MERCATOR_RANGE / 2);
    this.pixelsPerLonDegree_ = MERCATOR_RANGE / 360;
    this.pixelsPerLonRadian_ = MERCATOR_RANGE / (2 * Math.PI)
}
MercatorProjection.prototype.fromLatLngToPoint = function(g, i) {
    var h = this;
    var k = i || new google.maps.Point(0, 0);
    var j = h.pixelOrigin_;
    k.x = j.x + g.lng() * h.pixelsPerLonDegree_;
    var l = bound(Math.sin(degreesToRadians(g.lat())), -0.9999, 0.9999);
    k.y = j.y + 0.5 * Math.log((1 + l) / (1 - l)) * -h.pixelsPerLonRadian_;
    return k
};
MercatorProjection.prototype.fromDivPixelToLatLng = function(n, p) {
    var l = this;
    var m = l.pixelOrigin_;
    var o = Math.pow(2, p);
    var k = (n.x / o - m.x) / l.pixelsPerLonDegree_;
    var j = (n.y / o - m.y) / -l.pixelsPerLonRadian_;
    var i = radiansToDegrees(2 * Math.atan(Math.exp(j)) - Math.PI / 2);
    return new google.maps.LatLng(i, k)
};
MercatorProjection.prototype.fromDivPixelToSphericalMercator = function(l, p) {
    var k = this;
    var i = k.fromDivPixelToLatLng(l, p);
    var m = 6378137;
    var n = m * degreesToRadians(i.lng());
    var j = degreesToRadians(i.lat());
    var o = (m / 2) * Math.log((1 + Math.sin(j)) / (1 - Math.sin(j)));
    return new google.maps.Point(n, o)
};
MercatorProjection.prototype.fromLatLngToSphericalMercator = function(f) {
    var h = 6378137;
    var i = h * degreesToRadians(f.lng());
    var g = degreesToRadians(f.lat());
    var j = (h / 2) * Math.log((1 + Math.sin(g)) / (1 - Math.sin(g)));
    return new google.maps.Point(i, j)
};
MercatorProjection.prototype.fromSphericalMercatorToLatLng = function(g, h) {
    if(g < 180 && g > -180 && h < 90 && h > -90) {
        f = g;
        e = h
    } else {
        var f = (g / 20037508.34) * 180;
        var e = (h / 20037508.34) * 180;
        e = 180 / Math.PI * (2 * Math.atan(Math.exp(e * Math.PI / 180)) - Math.PI / 2)
    }
    return new google.maps.LatLng(e, f)
};
MercatorProjection.prototype.fromLatLngPointToSphericalMercator = function(g) {
    var h = 6378137;
    var i = h * degreesToRadians(g.easting);
    var f = degreesToRadians(g.northing);
    var j = (h / 2) * Math.log((1 + Math.sin(f)) / (1 - Math.sin(f)));
    return new google.maps.Point(i, j)
};
var PointLayer = function(F, L, y) {
        var K = L;
        var z;
        var x;
        var A;
        var H;
        var P = y;

        function C() {
            return F
        }
        function E() {
            return K
        }
        function O(a) {
            K = a
        }
        function M(a) {
            if(G(a)) {
                z = a
            } else {
                z = [a, a, a, a, a]
            }
        }
        function B() {
            if(z) {
                return z
            }
            var a = {
                url: _.first(K).imageUrl,
                height: 30,
                width: 30,
                opt_textColor: "#000000"
            };
            return [a, a, a, a, a]
        }
        function N(a) {
            H = a
        }
        function D() {
            return H
        }
        function I(a) {
            if(x != undefined) {
                x(a, C())
            }
        }
        function J(a) {
            if(A != undefined) {
                A(a, C())
            }
        }
        function v(a) {
            x = a
        }
        function w(a) {
            A = a
        }
        function G(a) {
            if(a.constructor.toString().indexOf("Array") == -1) {
                return false
            } else {
                return true
            }
        }
        return {
            getId: C,
            getPoints: E,
            getMaxZoom: D,
            setPoints: O,
            add_pointClicked: v,
            add_pointDoubleClicked: w,
            pointClicked: I,
            pointDoubleClicked: J,
            setMaxZoom: N
        }
    };

function CustomTileLayer(d, f, e) {
    this.map = d;
    this.minZoomLevel = f;
    this.maxZoomLevel = e;
    this.format = "image/png"
}
CustomTileLayer.prototype.isPng = function() {
    return this.format == "image/png"
};
CustomTileLayer.prototype.getOverlayOptions = function() {
    var customTileLayer = this;
    var imageMapTypeOptions = {
        getTileUrl: function(coord, zoom) {
            var pointOne = new google.maps.Point(coord.x * 256, (coord.y + 1) * 256);
            var pointTwo = new google.maps.Point((coord.x + 1) * 256, coord.y * 256);
            var merctor = new MercatorProjection();
            var r = merctor.fromDivPixelToLatLng(pointOne, zoom);
            var o = merctor.fromDivPixelToLatLng(pointTwo, zoom);
            if(o.lng() - r.lng() < 0) {
                o = new google.maps.LatLng(o.lat(), (o.lng() + 359.999))
            }
            var s = merctor.fromLatLngToSphericalMercator(r);
            var p = merctor.fromLatLngToSphericalMercator(o);
            var bbox = s.x + "," + s.y + "," + p.x + "," + p.y;
            var srs = "EPSG:900913";
            var fullUrl = customTileLayer.baseURL;
            fullUrl += "&REQUEST=GetMap";
            fullUrl += "&SERVICE=WMS";
            fullUrl += "&VERSION=1.1.1";
            if(customTileLayer.layers) {
                fullUrl += "&LAYERS=" + customTileLayer.layers
            }
            if(customTileLayer.styles) {
                fullUrl += "&STYLES=" + customTileLayer.styles
            }
            if(customTileLayer.sld) {
                fullUrl += "&SLD=" + customTileLayer.sld
            }
            fullUrl += "&FORMAT=" + customTileLayer.format;
            fullUrl += "&BGCOLOR=0xFFFFFF";
            fullUrl += "&TRANSPARENT=TRUE";
            fullUrl += "&SRS=" + srs;
            fullUrl += "&BBOX=" + bbox;
            fullUrl += "&WIDTH=" + 256;
            fullUrl += "&HEIGHT=" + 256;
            return fullUrl
        },
        tileSize: new google.maps.Size(256, 256),
        minZoom: this.minZoomLevel,
        maxZoom: this.maxZoomLevel,
        isPng: true
    };
    return imageMapTypeOptions
};
