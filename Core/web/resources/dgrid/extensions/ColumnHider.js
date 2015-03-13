//>>built
define("dgrid/extensions/ColumnHider","dojo/_base/declare dojo/has dojo/on ../util/misc put-selector/put dojo/i18n!./nls/columnHider xstyle/css!../css/extensions/ColumnHider.css".split(" "),function(r,h,f,k,g,s){var n,m,p=h("ie"),q=p&&h("quirks"),t=8>p||q?"htmlFor":"for";return r(null,{hiderMenuNode:null,hiderToggleNode:null,i18nColumnHider:s,_hiderMenuOpened:!1,_columnHiderRules:null,_columnHiderCheckboxes:null,_renderHiderMenuEntries:function(){var a=this.subRows,b=!0,c,d,e,l;delete this._columnHiderFirstCheckbox;
e=0;for(c=a.length;e<c;e++){l=0;for(d=a[e].length;l<d;l++)this._renderHiderMenuEntry(a[e][l]),b&&(b=!1,this._columnHiderFirstCheckbox=this._columnHiderCheckboxes[a[e][l].id])}},_renderHiderMenuEntry:function(a){var b=a.id,c=k.escapeCssIdentifier(b,"-"),d,e;a.hidden&&(a.hidden=!1,this._hideColumn(b),a.hidden=!0);a.unhidable||(d=g("div.dgrid-hider-menu-row"),e=this.domNode.id+"-hider-menu-check-"+c,b=this._columnHiderCheckboxes[b]=g(d,"input.dgrid-hider-menu-check.hider-menu-check-"+c+"[type\x3dcheckbox]"),
b.id=e,g(d,"label.dgrid-hider-menu-label.hider-menu-label-"+c+"["+t+"\x3d"+e+"]",a.label||a.field||""),g(this.hiderMenuNode,d),a.hidden||(b.checked=!0))},renderHeader:function(){function a(a){a.stopPropagation()}var b=this,c=this.hiderMenuNode,d=this.hiderToggleNode,e;this.inherited(arguments);if(c){for(e in this._columnHiderRules)this._columnHiderRules[e].remove();c.innerHTML=""}else d=this.hiderToggleNode=g(this.domNode,"button.ui-icon.dgrid-hider-toggle[type\x3dbutton][aria-label\x3d"+this.i18nColumnHider.popupTriggerLabel+
"]"),this._listeners.push(f(d,"click",function(a){b._toggleColumnHiderMenu(a)})),c=this.hiderMenuNode=g("div.dgrid-hider-menu[role\x3ddialog][aria-label\x3d"+this.i18nColumnHider.popupLabel+"]"),c.id=this.id+"-hider-menu",this._listeners.push(f(c,"keyup",function(a){if(27===(a.charCode||a.keyCode))b._toggleColumnHiderMenu(a),d.focus()})),c.style.display="none",g(this.domNode,c),this._listeners.push(f(c,".dgrid-hider-menu-check:"+(9>p||q?"click":"change"),function(a){b._updateColumnHiddenState(a.target.id.substr(b.id.length+
18),!a.target.checked)})),this._listeners.push(f(c,"mousedown",a),f(d,"mousedown",a)),m||(m=f.pausable(document,"mousedown",function(a){n&&n._toggleColumnHiderMenu(a)}),m.pause());this._columnHiderCheckboxes={};this._columnHiderRules={};this._renderHiderMenuEntries()},destroy:function(){this.inherited(arguments);for(var a in this._columnHiderRules)this._columnHiderRules[a].remove()},left:function(a,b){return this.right(a,-b)},right:function(a,b){a.element||(a=this.cell(a));for(var c=this.inherited(arguments),
d=a;c.column.hidden;){c=this.inherited(arguments,[c,0<b?1:-1]);if(d.element===c.element)return a;d=c}return c},isColumnHidden:function(a){return!!this._columnHiderRules[a]},_toggleColumnHiderMenu:function(){var a=this._hiderMenuOpened,b=this.hiderMenuNode,c=this.domNode,d;b.style.display=a?"none":"";a?b.style.height="":(b.offsetHeight>c.offsetHeight-12&&(b.style.height=c.offsetHeight-12+"px"),(d=this._columnHiderFirstCheckbox)&&d.focus());m[a?"pause":"resume"]();n=a?null:this;this._hiderMenuOpened=
!a},_hideColumn:function(a){var b=this,c="#"+k.escapeCssIdentifier(this.domNode.id)+" .dgrid-column-",d;if(!this._columnHiderRules[a]&&(this._columnHiderRules[a]=k.addCssRule(c+k.escapeCssIdentifier(a,"-"),"display: none;"),(8===h("ie")||10===h("ie"))&&!h("quirks")))d=k.addCssRule(".dgrid-row-table","display: inline-table;"),window.setTimeout(function(){d.remove();b.resize()},0)},_showColumn:function(a){this._columnHiderRules[a]&&(this._columnHiderRules[a].remove(),delete this._columnHiderRules[a])},
_updateColumnHiddenState:function(a,b){this[b?"_hideColumn":"_showColumn"](a);this.columns[a].hidden=b;f.emit(this.domNode,"dgrid-columnstatechange",{grid:this,column:this.columns[a],hidden:b,bubbles:!0});this.resize()},toggleColumnHiddenState:function(a,b){"undefined"===typeof b&&(b=!this._columnHiderRules[a]);this._updateColumnHiddenState(a,b);this._columnHiderCheckboxes[a].checked=!b}})});
//# sourceMappingURL=ColumnHider.js.map