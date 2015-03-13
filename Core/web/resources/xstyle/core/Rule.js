//>>built
define("xstyle/core/Rule",["xstyle/core/expression","xstyle/core/Definition","put-selector/put","xstyle/core/utils"],function(m,y,w,p){function n(a,b,c){try{a[b]=c}catch(d){}}function k(){}function x(a,b,c,d){if(a.calls){var e;for(c=0;c<a.length;c++){var f=a[c];if(f instanceof q){a.hasOwnProperty(c)||(a[c]=f=r(f));var g=f.ref&&(f.ref.selfResolving?f.ref.apply(b,f.getArgs(),b):m.evaluate(b,[f.caller,f]));void 0!==g&&((e||(e=[])).push(g),f.evaluated=!0)}}}if(e)return b=m.react(function(){for(var b=
0,c=a.slice(),d=0;d<a.length;d++){var e=a[d];e instanceof q&&e.evaluated&&(c[d-1]=a[d-1].slice(0,-e.caller.length),c[d]=arguments[b++])}return c.join("")}),b.skipResolve=!0,d=new y,d.setCompute(b.apply(d,e,d)),d;if(!d)return a.toString()}function q(a){this.caller=a;this.args=[]}function z(a,b,c,d){return p.when(b,function(b){var f=b;b&&b.forRule&&((a._subRuleListeners||(a._subRuleListeners=[])).push(function(a){var b=f.forRule(a,!0);b&&b.forElement?t(a,b,d):c&&c(b)}),b=b.forRule(a));if(b&&b.forElement)return t(a,
b,d);c&&c(b)})}function t(a,b,c){return require(["xstyle/core/elemental"],function(d){d.addRenderer(a,function(a){var d=b.forElement(a);c&&c(d,a)})})}var r=Object.create||function(a){function b(){}b.prototype=a;return new b},s=p.convertCssNameToJs,A={"{":"}","[":"]","(":")"},u=w("div").style;k.prototype={property:function(a){return(this._properties||(this._properties={}))[a]||(this._properties[a]=new Proxy(this.get(a)))},eachProperty:function(a){for(var b=this.values||0,c=0;c<b.length;c++){var d=
b[c];a.call(this,d||"unnamed",b[d])}},fullSelector:function(){return(this.parent?this.parent.fullSelector():"")+(this.selector||"")+" "},newRule:function(a){a=(this.rules||(this.rules={}))[a]=new k;a.disabled=this.disabled;a.parent=this;return a},newCall:function(a){a=new q(a);a.parent=this;return a},addSheetRule:function(a,b){if("@"!=a.charAt(0)){var c=this.styleSheet,d=c.cssRules||c.rules,e=-1<this.ruleIndex?this.ruleIndex:d.length;try{c.addRule(a,b||" ",e)}catch(f){a.match(/-(moz|webkit|ie)-/)}return d[e]}},
onRule:function(){var a=this.getCssRule();if(this.installStyles)for(var b=0;b<this.installStyles.length;b++){var c=this.installStyles[b];n(a.style,c[0],c[1])}},setStyle:function(a,b){this.cssRule?n(this.cssRule.style,a,b):(this.installStyles||(this.installStyles=[])).push([a,b])},getCssRule:function(){this.cssRule||(this.cssRule=this.addSheetRule(this.selector,this.cssText));return this.cssRule},get:function(a){return this.values[a]},elements:function(a){var b=this;require(["xstyle/core/elemental"],
function(c){c.addRenderer(b,function(b){a(b)})})},declareDefinition:function(a,b,c){a=a&&s(a);if(!this.disabled){var d=this;if(b.length)if("\x3e"==b[0].toString().charAt(0))a||(this.generator=b,require(["xstyle/core/generate","xstyle/core/elemental"],function(a,c){b=a.forSelector(b,d);c.addRenderer(d,b)}));else{var e=a in u||this.getDefinition(a);if(!c||!e){c=this.definitions||(this.definitions={});e=b[0];if(e.indexOf&&-1<e.indexOf(","))for(var e=b.join("").split(/\s*,\s*/),f=[],g=0;g<e.length;g++)f[g]=
m.evaluate(this,e[g]);b[0]&&"{"==b[0].operator?f=b[0]:b[1]&&"{"==b[1].operator&&(f=b[1]);f=f||m.evaluate(this,b);if(f.then)var h=f,f={then:function(a){return h.then(function(b){return a(l(b))})}};var l=function(b){b.define&&(b=b.define(d,a));return b};return c[a]=l(f)}}else return c=this.definitions||(this.definitions={}),c[a]=b}},onArguments:function(a){var b=a.ref;return b&&b.apply(this,a.getArgs(),this)},setValue:function(a,b,c){var d=s(a);if(!this.disabled){var e=this.values||(this.values=[]);
e.push(d);e[d]=b;if(a){do{e=(c||this).getDefinition(a);if(void 0!==e){if(this.cssRule&&(!e||!e.keepCSSValue)){var f=this.cssRule.style;d in f&&n(f,d,"")}z(this,e.put(b,this,d))}a=a.substring(0,a.lastIndexOf("-"))}while(a)}d in u&&this._setStyleFromValue(d,b,!0)}},_setStyleFromValue:function(a,b,c){var d=b[0];if(d instanceof k){c=d.values;for(d=0;d<c.length;d++){var e=c[d];this._setStyleFromValue(a+("main"==e?"":e.charAt(0).toUpperCase()+e.slice(1)),c[e])}}else{if(b.calls){var f=this;if(b.expression=
x(b,this,a,!0)){var g=b.expression&&b.expression.valueOf(),h=function(b,c){var d=g&&g.forRule?g.forRule(b,!0):g;if(d&&d.forElement){var e=c&&c.elements;if(e)for(var f=0;f<e.length;f++)for(var l=e[f].querySelectorAll(b.selector),h=0;h<l.length;h++){var k=l[h];n(k.style,a,d.forElement(k))}else t(b,d,function(b,c){n(c.style,a,b)})}else b.setStyle(a,d)},l=[f];p.when(g,function(a){if((g=a)&&g.forRule)(f._subRuleListeners||(f._subRuleListeners=[])).push(function(a){l.push(a);h(a)});h(f)});b.expression.depend({invalidate:function(a){p.when(b.expression.valueOf(),
function(b){g=b;for(b=0;b<l.length;b++){var c=!0,d=l[b];if(a&&a.rules)for(var c=!1,e=0;e<a.rules.length;e++)if(a.rules[e]===d){c=!0;break}c&&h(d,a)}})}})}}c||this.setStyle(a,b)}},put:function(a){var b=this;return{forRule:function(c){b.extend(c);if("defaults"!=a&&a&&"string"==typeof a&&b.values)for(var d=a.toString().split(/,\s*/),e=0;e<d.length;e++){var f=b.values[e];f&&c.setValue(f,d[e],b)}}}},extend:function(a,b){(this.derivatives||(this.derivatives=[])).push(a);var c=this.extraSelector;c&&(a.selector+=
c);for(var d=this.cssRule.style,e=a.getCssRule().style,f=a.inheritedStyles||(a.inheritedStyles={}),c=0;c<d.length;c++){var g=s(d[c]);if(!e[g]||f[g])e[g]=d[g],f[g]=!0}this.values&&(a.values=r(this.values));if(b){if(c=this.definitions)a.definitions=r(c);a.tagName=this.tagName||a.tagName}(a.bases||(a.bases=[])).push(this);d=this._subRuleListeners||0;for(c=0;c<d.length;c++)d[c](a);var h=a.getCssRule().style;this.eachProperty(function(b,c){"object"==typeof c&&(c=r(c));b in u&&!h[b]&&a._setStyleFromValue(b,
c)});if(d=this.generator){if(d instanceof Array){d=d.slice(0);for(c=0;c<d.length;c++)e=d[c],"{"===e.operator&&(f=a.newRule(),f.selector=e.selector+a.selector.slice(1),f.styleSheet=a.styleSheet||a.cssRule.parentStyleSheet,e.extend(f,!0),d[c]=f)}a.declareDefinition(null,d)}},getDefinition:function(a,b){a=s(a);var c=this;do{var d=c.definitions&&c.definitions[a];void 0===d&&(b&&c[b])&&(d=c[b][a]);c=c.parent}while(void 0===d&&c);return d},newElement:function(){return w((this.tagName||"span")+(this.selector||
""))},cssText:""};m.evaluateText=x;var v=q.prototype=new k;v.declareDefinition=v.setValue=function(a,b){this.args.push(b)};v.toString=function(){var a=this.operator;return a+this.args+A[a]};k.updateStaleProperties=function(){};return k});
//# sourceMappingURL=Rule.js.map