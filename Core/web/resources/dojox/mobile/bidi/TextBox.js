//>>built
define("dojox/mobile/bidi/TextBox",["dojo/_base/declare","dijit/_BidiSupport"],function(b){return b(null,{_setTextDirAttr:function(a){if(!this._created||this.textDir!=a)this._set("textDir",a),this.value?this.applyTextDir(this.focusNode||this.textbox):this.applyTextDir(this.focusNode||this.textbox,this.textbox.getAttribute("placeholder"))},_setDirAttr:function(a){if(!this.textDir||!this.textbox)this.dir=a},_onBlur:function(a){this.inherited(arguments);this.textbox.value||this.applyTextDir(this.textbox,
this.textbox.getAttribute("placeholder"))},_onInput:function(a){this.inherited(arguments);this.textbox.value||this.applyTextDir(this.textbox,this.textbox.getAttribute("placeholder"))}})});
//@ sourceMappingURL=TextBox.js.map