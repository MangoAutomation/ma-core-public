//>>built
define("dstore/QueryResults",["dojo/_base/lang","dojo/when"],function(e,f){function g(a,b){return f(this,function(d){for(var c=0,h=d.length;c<h;c++)a.call(b,d[c],c,d)})}return function(a,b){var d=b&&"totalLength"in b;if(a.then){a=e.delegate(a);var c=a.then(function(a){var c=d?b.totalLength:a.totalLength||a.length;return a.totalLength=c});a.totalLength=d?b.totalLength:c;a.response=b&&b.response}else a.totalLength=d?b.totalLength:a.length;a.forEach=g;return a}});
//# sourceMappingURL=QueryResults.js.map