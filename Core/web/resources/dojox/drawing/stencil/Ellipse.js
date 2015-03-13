//>>built
define("dojox/drawing/stencil/Ellipse",["dojo/_base/lang","../util/oo","./_Base","../manager/_registry"],function(e,c,f,g){c=c.declare(f,function(a){},{type:"dojox.drawing.stencil.Ellipse",anchorType:"group",baseRender:!0,dataToPoints:function(a){a=a||this.data;var b=a.cx-a.rx,d=a.cy-a.ry,c=2*a.rx;a=2*a.ry;return this.points=[{x:b,y:d},{x:b+c,y:d},{x:b+c,y:d+a},{x:b,y:d+a}]},pointsToData:function(a){a=a||this.points;var b=a[0];a=a[2];return this.data={cx:b.x+(a.x-b.x)/2,cy:b.y+(a.y-b.y)/2,rx:0.5*
(a.x-b.x),ry:0.5*(a.y-b.y)}},_create:function(a,b,c){this.remove(this[a]);this[a]=this.container.createEllipse(b).setStroke(c).setFill(c.fill);this._setNodeAtts(this[a])},render:function(){this.onBeforeRender(this);this.renderHit&&this._create("hit",this.data,this.style.currentHit);this._create("shape",this.data,this.style.current)}});e.setObject("dojox.drawing.stencil.Ellipse",c);g.register({name:"dojox.drawing.stencil.Ellipse"},"stencil");return c});
//# sourceMappingURL=Ellipse.js.map