//>>built
define("dojox/mobile/Accordion","dojo/_base/array dojo/_base/declare dojo/_base/lang dojo/sniff dojo/dom-class dojo/dom-construct dojo/dom-attr dijit/_Contained dijit/_Container dijit/_WidgetBase ./iconUtils ./lazyLoadUtils ./_css3 ./common require dojo/has!dojo-bidi?dojox/mobile/bidi/Accordion".split(" "),function(f,m,r,l,g,k,c,h,s,n,t,u,p,v,x,w){var q=m([n,h],{label:"Label",icon1:"",icon2:"",iconPos1:"",iconPos2:"",selected:!1,baseClass:"mblAccordionTitle",buildRendering:function(){this.inherited(arguments);
var a=this.anchorNode=k.create("a",{className:"mblAccordionTitleAnchor",role:"presentation"},this.domNode);this.textBoxNode=k.create("div",{className:"mblAccordionTitleTextBox"},a);this.labelNode=k.create("span",{className:"mblAccordionTitleLabel",innerHTML:this._cv?this._cv(this.label):this.label},this.textBoxNode);this._isOnLine=this.inheritParams();c.set(this.textBoxNode,"role","tab");c.set(this.textBoxNode,"tabindex","0")},postCreate:function(){this.connect(this.domNode,"onclick","_onClick");
v.setSelectable(this.domNode,!1)},inheritParams:function(){var a=this.getParent();a&&(this.icon1&&(a.iconBase&&"/"===a.iconBase.charAt(a.iconBase.length-1))&&(this.icon1=a.iconBase+this.icon1),this.icon1||(this.icon1=a.iconBase),this.iconPos1||(this.iconPos1=a.iconPos),this.icon2&&(a.iconBase&&"/"===a.iconBase.charAt(a.iconBase.length-1))&&(this.icon2=a.iconBase+this.icon2),this.icon2||(this.icon2=a.iconBase||this.icon1),this.iconPos2||(this.iconPos2=a.iconPos||this.iconPos1));return!!a},_setIcon:function(a,
b){this.getParent()&&(this._set("icon"+b,a),this["iconParentNode"+b]||(this["iconParentNode"+b]=k.create("div",{className:"mblAccordionIconParent mblAccordionIconParent"+b},this.anchorNode,"first")),this["iconNode"+b]=t.setIcon(a,this["iconPos"+b],this["iconNode"+b],this.alt,this["iconParentNode"+b]),this["icon"+b]=a,g.toggle(this.domNode,"mblAccordionHasIcon",a&&"none"!==a),l("dojo-bidi")&&!this.getParent().isLeftToRight()&&this.getParent()._setIconDir(this["iconParentNode"+b]))},_setIcon1Attr:function(a){this._setIcon(a,
1)},_setIcon2Attr:function(a){this._setIcon(a,2)},startup:function(){this._started||(this._isOnLine||this.inheritParams(),this._isOnLine||this.set({icon1:this.icon1,icon2:this.icon2}),this.inherited(arguments))},_onClick:function(a){!1!==this.onClick(a)&&(a=this.getParent(),!a.fixedHeight&&"none"!==this.contentWidget.domNode.style.display?a.collapse(this.contentWidget,!a.animation):a.expand(this.contentWidget,!a.animation))},onClick:function(){},_setSelectedAttr:function(a){g.toggle(this.domNode,
"mblAccordionTitleSelected",a);this._set("selected",a)}});h=m(l("dojo-bidi")?"dojox.mobile.NonBidiAccordion":"dojox.mobile.Accordion",[n,s,h],{iconBase:"",iconPos:"",fixedHeight:!1,singleOpen:!1,animation:!0,roundRect:!1,duration:0.3,baseClass:"mblAccordion",_openSpace:1,buildRendering:function(){this.inherited(arguments);c.set(this.domNode,"role","tablist");c.set(this.domNode,"aria-multiselectable",!this.singleOpen)},startup:function(){if(!this._started){g.contains(this.domNode,"mblAccordionRoundRect")?
this.roundRect=!0:this.roundRect&&g.add(this.domNode,"mblAccordionRoundRect");this.fixedHeight&&(this.singleOpen=!0);var a=this.getChildren();f.forEach(a,this._setupChild,this);var b,d=1;f.forEach(a,function(e){e.startup();e._at.startup();this.collapse(e,!0);c.set(e._at.textBoxNode,"aria-setsize",a.length);c.set(e._at.textBoxNode,"aria-posinset",d++);e.selected&&(b=e)},this);!b&&this.fixedHeight&&(b=a[a.length-1]);b?this.expand(b,!0):this._updateLast();this.defer(function(){this.resize()});this._started=
!0}},_setupChild:function(a){"hidden"!=a.domNode.style.overflow&&(a.domNode.style.overflow=this.fixedHeight?"auto":"hidden");a._at=new q({label:a.label,alt:a.alt,icon1:a.icon1,icon2:a.icon2,iconPos1:a.iconPos1,iconPos2:a.iconPos2,contentWidget:a});k.place(a._at.domNode,a.domNode,"before");g.add(a.domNode,"mblAccordionPane");c.set(a._at.textBoxNode,"aria-controls",a.domNode.id);c.set(a.domNode,"role","tabpanel");c.set(a.domNode,"aria-labelledby",a._at.id)},addChild:function(a,b){this.inherited(arguments);
this._started&&(this._setupChild(a),a._at.startup(),a.selected?(this.expand(a,!0),this.defer(function(){a.domNode.style.height=""})):this.collapse(a),this._addChildAriaAttrs())},removeChild:function(a){"number"==typeof a&&(a=this.getChildren()[a]);a&&a._at.destroy();this.inherited(arguments);this._addChildAriaAttrs()},_addChildAriaAttrs:function(){var a=1,b=this.getChildren();f.forEach(b,function(d){c.set(d._at.textBoxNode,"aria-posinset",a++);c.set(d._at.textBoxNode,"aria-setsize",b.length)})},getChildren:function(){return f.filter(this.inherited(arguments),
function(a){return!(a instanceof q)})},getSelectedPanes:function(){return f.filter(this.getChildren(),function(a){return"none"!=a.domNode.style.display})},resize:function(){if(this.fixedHeight){var a=f.filter(this.getChildren(),function(a){return"none"!=a._at.domNode.style.display}),b=this.domNode.clientHeight;f.forEach(a,function(a){b-=a._at.domNode.offsetHeight});this._openSpace=0<b?b:0;a=this.getSelectedPanes()[0];a.domNode.style[p.name("transition")]="";a.domNode.style.height=this._openSpace+
"px"}},_updateLast:function(){var a=this.getChildren();f.forEach(a,function(b,c){g.toggle(b._at.domNode,"mblAccordionTitleLast",c===a.length-1&&!g.contains(b._at.domNode,"mblAccordionTitleSelected"))},this)},expand:function(a,b){a.lazy&&(u.instantiateLazyWidgets(a.containerNode,a.requires),a.lazy=!1);var d=this.getChildren();f.forEach(d,function(e,c){e.domNode.style[p.name("transition")]=b?"":"height "+this.duration+"s linear";if(e===a){e.domNode.style.display="";var d;this.fixedHeight?d=this._openSpace:
(d=parseInt(e.height||e.domNode.getAttribute("height")),d||(e.domNode.style.height="",d=e.domNode.offsetHeight,e.domNode.style.height="0px"));this.defer(function(){e.domNode.style.height=d+"px"});this.select(a)}else this.singleOpen&&this.collapse(e,b)},this);this._updateLast();c.set(a.domNode,"aria-expanded","true");c.set(a.domNode,"aria-hidden","false")},collapse:function(a,b){if("none"!==a.domNode.style.display){a.domNode.style[p.name("transition")]=b?"":"height "+this.duration+"s linear";a.domNode.style.height=
"0px";if(!l("css3-animations")||b)a.domNode.style.display="none",this._updateLast();else{var d=this;d.defer(function(){a.domNode.style.display="none";d._updateLast();if(!d.fixedHeight&&d.singleOpen)for(var b=d.getParent();b;b=b.getParent())if(g.contains(b.domNode,"mblView")){b&&b.resize&&b.resize();break}},1E3*this.duration)}this.deselect(a);c.set(a.domNode,"aria-expanded","false");c.set(a.domNode,"aria-hidden","true")}},select:function(a){a._at.set("selected",!0);c.set(a._at.textBoxNode,"aria-selected",
"true")},deselect:function(a){a._at.set("selected",!1);c.set(a._at.textBoxNode,"aria-selected","false")}});h.ChildWidgetProperties={alt:"",label:"",icon1:"",icon2:"",iconPos1:"",iconPos2:"",selected:!1,lazy:!1};r.extend(n,h.ChildWidgetProperties);return l("dojo-bidi")?m("dojox.mobile.Accordion",[h,w]):h});
//# sourceMappingURL=Accordion.js.map