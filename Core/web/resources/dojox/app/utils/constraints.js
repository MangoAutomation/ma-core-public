//>>built
define("dojox/app/utils/constraints",["dojo/_base/array"],function(g){var f=[];return{getSelectedChild:function(b,a){var c=typeof a,c="string"==c||"number"==c?a:a.__hash;return b&&b.selectedChildren&&b.selectedChildren[c]?b.selectedChildren[c]:null},setSelectedChild:function(b,a,c){var d=typeof a;b.selectedChildren["string"==d||"number"==d?a:a.__hash]=c},getAllSelectedChildren:function(b,a){a=a||[];if(b&&b.selectedChildren)for(var c in b.selectedChildren)if(b.selectedChildren[c]){var d=b.selectedChildren[c];
a.push(d);this.getAllSelectedChildren(d,a)}return a},register:function(b){var a=typeof b;if(!b.__hash&&"string"!=a&&"number"!=a){var c=null;g.some(f,function(a){var d=!0,e;for(e in a)if("_"!==e.charAt(0)&&a[e]!=b[e]){d=!1;break}!0==d&&(c=a);return d});if(c)b.__hash=c.__hash;else{var a="",d;for(d in b)"_"!==d.charAt(0)&&(a+=b[d]);b.__hash=a;f.push(b)}}}}});
//# sourceMappingURL=constraints.js.map