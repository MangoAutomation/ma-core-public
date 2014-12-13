//>>built
define("xstyle/shim/var",[],function(e){return{onFunction:function(c,a,b){a=b;do{var d=a.variables&&a.variables[c];a=a.parent}while(!d);b.addSheetRule(b.selector,c+": "+b.get(c).replace(/var\([^)]+\)/g,d))}}});
//@ sourceMappingURL=var.js.map