var KanjiViewer = {
    initialize: function(divName, strokeWidth, fontSize) {
        this.paper = new Raphael(divName, '100%', '100%').initZoom();
        this.strokes = this.paper.set();
        this.orders = this.paper.set();
        this.strokeWidth = strokeWidth;
        this.fontSize = fontSize;
        this.zoomFactor = 1;
    },
    draw: function(kanjis) {
    	this.strokes = this.paper.set();
        this.orders = this.paper.set();
        this.paper.clear();
        this.paper.initZoom();
        
    	var kanjisCount = kanjis.length;
        for (var k = 0; k < kanjisCount; ++k) {
	    Raphael.getColor.reset();
	    var strokesCount = kanjis[k].paths.length;
        for (var i = 0; i < strokesCount; ++i) {
            var stroke = this.paper.path(kanjis[k].paths[i]).initZoom();
            this.strokes.push(stroke);
            var color = Raphael.getColor();
            stroke.setAttr({'stroke': color});
            stroke.setAttr({'stroke-width': this.strokeWidth});
            stroke.setAttr({'stroke-linecap': 'round'});
            stroke.setAttr({'stroke-linejoin': 'round'});
            stroke.translate(k * 109, 0);
            //if (kanjis[k].orders != undefined) {
            //	if (kanjis[k].orders[i] != undefined) {
            //		var order = this.paper.text(kanjis[k].orders[i][0], kanjis[k].orders[i][1], kanjis[k].orders[i][2]).initZoom();
            //		this.orders.push(order);
            //		order.setAttr({fill: color, 'font-size': fontSize});
            //		order.translate(k * 109, 0);
            // 	}
            //}
        }
        }
    }, 
    clear: function() {
    	this.strokes = this.paper.set();
        this.orders = this.paper.set();
        this.paper.clear();
    },
    setZoom: function(zoom) {
        this.paper.setZoom(zoom);
    },
    setStrokeWidth: function(strokeWidth) {
    	this.strokeWidth = strokeWidth;
        var strokesCount = this.strokes.length;
        for (var i = 0; i < strokesCount; ++i) {
            var stroke = this.strokes[i];
            stroke.setAttr({'stroke-width': strokeWidth });
        }
    },
    setFontSize: function(fontSize) {
        this.fontSize = fontSize;
        var ordersCount = this.orders.length;
        for (var i = 0; i < ordersCount; ++i) {
            var order = this.orders[i];
            order.setAttr({'font-size': fontSize});
        }
    }
};
