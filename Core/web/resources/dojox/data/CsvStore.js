//>>built
define("dojox/data/CsvStore","dojo/_base/lang dojo/_base/declare dojo/_base/xhr dojo/_base/kernel dojo/data/util/filter dojo/data/util/simpleFetch".split(" "),function(n,l,p,s,q,t){l=l("dojox.data.CsvStore",null,{constructor:function(a){this._attributes=[];this._attributeIndexes={};this._dataArray=[];this._arrayOfAllItems=[];this._loadFinished=!1;a.url&&(this.url=a.url);this._csvData=a.data;a.label?this.label=a.label:""===this.label&&(this.label=void 0);this._storeProp="_csvStore";this._idProp="_csvId";
this._features={"dojo.data.api.Read":!0,"dojo.data.api.Identity":!0};this._loadInProgress=!1;this._queuedFetches=[];this.identifier=a.identifier;""===this.identifier?delete this.identifier:this._idMap={};"separator"in a&&(this.separator=a.separator);"urlPreventCache"in a&&(this.urlPreventCache=a.urlPreventCache?!0:!1)},url:"",label:"",identifier:"",separator:",",urlPreventCache:!1,_assertIsItem:function(a){if(!this.isItem(a))throw Error(this.declaredClass+": a function was passed an item argument that was not an item");
},_getIndex:function(a){a=this.getIdentity(a);this.identifier&&(a=this._idMap[a]);return a},getValue:function(a,b,c){this._assertIsItem(a);var d=c;if("string"===typeof b)b=this._attributeIndexes[b],null!=b&&(d=this._dataArray[this._getIndex(a)][b]||c);else throw Error(this.declaredClass+": a function was passed an attribute argument that was not a string");return d},getValues:function(a,b){var c=this.getValue(a,b);return c?[c]:[]},getAttributes:function(a){this._assertIsItem(a);var b=[];a=this._dataArray[this._getIndex(a)];
for(var c=0;c<a.length;c++)""!==a[c]&&b.push(this._attributes[c]);return b},hasAttribute:function(a,b){this._assertIsItem(a);if("string"===typeof b){var c=this._attributeIndexes[b],d=this._dataArray[this._getIndex(a)];return"undefined"!==typeof c&&c<d.length&&""!==d[c]}throw Error(this.declaredClass+": a function was passed an attribute argument that was not a string");},containsValue:function(a,b,c){var d=void 0;"string"===typeof c&&(d=q.patternToRegExp(c,!1));return this._containsValue(a,b,c,d)},
_containsValue:function(a,b,c,d){a=this.getValues(a,b);for(b=0;b<a.length;++b){var e=a[b];if("string"===typeof e&&d)return null!==e.match(d);if(c===e)return!0}return!1},isItem:function(a){if(a&&a[this._storeProp]===this)if(a=a[this._idProp],this.identifier){if(this._dataArray[this._idMap[a]])return!0}else if(0<=a&&a<this._dataArray.length)return!0;return!1},isItemLoaded:function(a){return this.isItem(a)},loadItem:function(a){},getFeatures:function(){return this._features},getLabel:function(a){if(this.label&&
this.isItem(a))return this.getValue(a,this.label)},getLabelAttributes:function(a){return this.label?[this.label]:null},_fetchItems:function(a,b,c){var d=this,e=function(a,c){var f=null;if(a.query){var e,g,f=[],h=a.queryOptions?a.queryOptions.ignoreCase:!1,m={};for(e in a.query)g=a.query[e],"string"===typeof g&&(m[e]=q.patternToRegExp(g,h));for(h=0;h<c.length;++h){var l=!0,n=c[h];for(e in a.query)g=a.query[e],d._containsValue(n,e,g,m[e])||(l=!1);l&&f.push(n)}}else f=c.slice(0,c.length);b(f,a)};if(this._loadFinished)e(a,
this._arrayOfAllItems);else if(""!==this.url)if(this._loadInProgress)this._queuedFetches.push({args:a,filter:e});else{this._loadInProgress=!0;var g=p.get({url:d.url,handleAs:"text",preventCache:d.urlPreventCache});g.addCallback(function(b){try{d._processData(b),e(a,d._arrayOfAllItems),d._handleQueuedFetches()}catch(f){c(f,a)}});g.addErrback(function(b){d._loadInProgress=!1;if(c)c(b,a);else throw b;});var f=null;a.abort&&(f=a.abort);a.abort=function(){g&&-1===g.fired&&g.cancel();f&&f.call(a)}}else if(this._csvData)try{this._processData(this._csvData),
this._csvData=null,e(a,this._arrayOfAllItems)}catch(h){c(h,a)}else{var m=Error(this.declaredClass+": No CSV source data was provided as either URL or String data input.");if(c)c(m,a);else throw m;}},close:function(a){},_getArrayOfArraysFromCsvFileContents:function(a){if(n.isString(a)){var b=RegExp("^\\s+","g"),c=RegExp("\\s+$","g"),d=RegExp('""',"g"),e=[],g=this._splitLines(a);for(a=0;a<g.length;++a){var f=g[a];if(0<f.length){for(var f=f.split(this.separator),h=0;h<f.length;){var m=f[h].replace(b,
""),k=m.replace(c,""),l=k.charAt(0),r=k.charAt(k.length-1),p=k.charAt(k.length-2),q=k.charAt(k.length-3);if(2===k.length&&'""'==k)f[h]="";else if('"'==l&&('"'!=r||'"'==r&&'"'==p&&'"'!=q)){if(h+1===f.length)return;f[h]=m+this.separator+f[h+1];f.splice(h+1,1)}else'"'==l&&'"'==r&&(k=k.slice(1,k.length-1),k=k.replace(d,'"')),f[h]=k,h+=1}e.push(f)}}this._attributes=e.shift();for(a=0;a<this._attributes.length;a++)this._attributeIndexes[this._attributes[a]]=a;this._dataArray=e}},_splitLines:function(a){var b=
[],c,d="",e=!1;for(c=0;c<a.length;c++){var g=a.charAt(c);switch(g){case '"':e=!e;d+=g;break;case "\r":e?d+=g:(b.push(d),d="",c<a.length-1&&"\n"==a.charAt(c+1)&&c++);break;case "\n":e?d+=g:(b.push(d),d="");break;default:d+=g}}""!==d&&b.push(d);return b},_processData:function(a){this._getArrayOfArraysFromCsvFileContents(a);this._arrayOfAllItems=[];if(this.identifier&&void 0===this._attributeIndexes[this.identifier])throw Error(this.declaredClass+": Identity specified is not a column header in the data set.");
for(a=0;a<this._dataArray.length;a++){var b=a;this.identifier&&(b=this._dataArray[a][this._attributeIndexes[this.identifier]],this._idMap[b]=a);this._arrayOfAllItems.push(this._createItemFromIdentity(b))}this._loadFinished=!0;this._loadInProgress=!1},_createItemFromIdentity:function(a){var b={};b[this._storeProp]=this;b[this._idProp]=a;return b},getIdentity:function(a){return this.isItem(a)?a[this._idProp]:null},fetchItemByIdentity:function(a){var b,c=a.scope?a.scope:s.global;if(this._loadFinished)b=
this._createItemFromIdentity(a.identity),this.isItem(b)||(b=null),a.onItem&&a.onItem.call(c,b);else{var d=this;if(""!==this.url)this._loadInProgress?this._queuedFetches.push({args:a}):(this._loadInProgress=!0,b=p.get({url:d.url,handleAs:"text"}),b.addCallback(function(b){try{d._processData(b);var e=d._createItemFromIdentity(a.identity);d.isItem(e)||(e=null);a.onItem&&a.onItem.call(c,e);d._handleQueuedFetches()}catch(h){a.onError&&a.onError.call(c,h)}}),b.addErrback(function(b){this._loadInProgress=
!1;a.onError&&a.onError.call(c,b)}));else if(this._csvData)try{d._processData(d._csvData),d._csvData=null,b=d._createItemFromIdentity(a.identity),d.isItem(b)||(b=null),a.onItem&&a.onItem.call(c,b)}catch(e){a.onError&&a.onError.call(c,e)}}},getIdentityAttributes:function(a){return this.identifier?[this.identifier]:null},_handleQueuedFetches:function(){if(0<this._queuedFetches.length){for(var a=0;a<this._queuedFetches.length;a++){var b=this._queuedFetches[a],c=b.filter,d=b.args;c?c(d,this._arrayOfAllItems):
this.fetchItemByIdentity(b.args)}this._queuedFetches=[]}}});n.extend(l,t);return l});
//@ sourceMappingURL=CsvStore.js.map