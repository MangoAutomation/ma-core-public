//>>built
define("xstyle/util/createStyleSheet",[],function(){var b=document.head;return function(c){if(document.createStyleSheet){var a=document.createStyleSheet();a.cssText=c;return a.owningElement}a=document.createElement("style");a.setAttribute("type","text/css");a.appendChild(document.createTextNode(c));b.insertBefore(a,b.firstChild);return a}});
//@ sourceMappingURL=createStyleSheet.js.map