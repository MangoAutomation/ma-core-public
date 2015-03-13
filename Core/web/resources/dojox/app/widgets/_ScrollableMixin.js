//>>built
define("dojox/app/widgets/_ScrollableMixin","dojo/_base/declare dojo/_base/lang dojo/_base/array dojo/_base/window dojo/dom-class dijit/registry dojo/dom dojo/dom-construct dojox/mobile/scrollable".split(" "),function(k,p,l,e,f,g,h,m,n){return k("dojox.app.widgets._ScrollableMixin",n,{scrollableParams:null,appBars:!0,allowNestedScrolls:!0,constructor:function(){this.scrollableParams={noResize:!0}},destroy:function(){this.cleanup();this.inherited(arguments)},startup:function(){if(!this._started){this.findAppBars();
var a,b=this.scrollableParams;this.fixedHeader&&(a=h.byId(this.fixedHeader),a.parentNode==this.domNode&&(this.isLocalHeader=!0),b.fixedHeaderHeight=a.offsetHeight);if(this.fixedFooter&&(a=h.byId(this.fixedFooter)))a.parentNode==this.domNode&&(this.isLocalFooter=!0,a.style.bottom="0px"),b.fixedFooterHeight=a.offsetHeight;this.init(b);this.inherited(arguments);this.reparent()}},buildRendering:function(){this.inherited(arguments);f.add(this.domNode,"mblScrollableView");this.domNode.style.overflow="hidden";
this.domNode.style.top="0px";this.containerNode=m.create("div",{className:"mblScrollableViewContainer"},this.domNode);this.containerNode.style.position="absolute";this.containerNode.style.top="0px";"v"===this.scrollDir&&(this.containerNode.style.width="100%")},reparent:function(){var a,b,c,d;b=a=0;for(c=this.domNode.childNodes.length;a<c;a++)d=this.domNode.childNodes[b],d===this.containerNode||this.checkFixedBar(d,!0)?b++:this.containerNode.appendChild(this.domNode.removeChild(d))},resize:function(){this.inherited(arguments);
l.forEach(this.getChildren(),function(a){a.resize&&a.resize()})},findAppBars:function(){if(this.appBars){var a,b,c;a=0;for(b=e.body().childNodes.length;a<b;a++)c=e.body().childNodes[a],this.checkFixedBar(c,!1);if(this.domNode.parentNode){a=0;for(b=this.domNode.parentNode.childNodes.length;a<b;a++)c=this.domNode.parentNode.childNodes[a],this.checkFixedBar(c,!1)}this.fixedFooterHeight=this.fixedFooter?this.fixedFooter.offsetHeight:0}},checkFixedBar:function(a,b){if(1===a.nodeType){var c=a.getAttribute("data-app-constraint")||
g.byNode(a)&&g.byNode(a)["data-app-constraint"];if("bottom"===c)return f.add(a,"mblFixedBottomBar"),b?this.fixedFooter=a:this._fixedAppFooter=a,c}return null}})});
//# sourceMappingURL=_ScrollableMixin.js.map