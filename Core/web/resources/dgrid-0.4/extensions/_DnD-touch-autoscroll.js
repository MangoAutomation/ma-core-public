//>>built
define("dgrid-0.4/extensions/_DnD-touch-autoscroll",["dojo/aspect","dojo/dom-geometry","dojo/dnd/autoscroll","../List"],function(m,q,e,n){var r=e.autoScrollNodes,c,p;c={};m.after(n.prototype,"postCreate",function(b){c[this.id]=this;return b});m.after(n.prototype,"destroy",function(b){delete c[this.id];return b});p=function(b){for(var a,d;b;){if((a=b.id)&&(d=c[a]))return d;b=b.parentNode}};e.autoScrollNodes=function(b){var a=b.target,d=p(a),c,l,f,g,h,k;if(d&&(a=d.touchNode.parentNode,a=q.position(a,
!0),c=b.pageX-a.x,l=b.pageY-a.y,f=Math.min(e.H_TRIGGER_AUTOSCROLL,a.w/2),g=Math.min(e.V_TRIGGER_AUTOSCROLL,a.h/2),c<f?h=-f:c>a.w-f&&(h=f),l<g?k=-g:l>a.h-g&&(k=g),h||k)){b=d.getScrollPosition();a={};h&&(a.x=b.x+h);k&&(a.y=b.y+k);d.scrollTo(a);return}r.call(this,b)};return e});
//# sourceMappingURL=_DnD-touch-autoscroll.js.map