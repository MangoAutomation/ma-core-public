//>>built
define("dojox/lang/functional/linrec",["dijit","dojo","dojox","dojo/require!dojox/lang/functional/lambda,dojox/lang/functional/util"],function(s,h,r){h.provide("dojox.lang.functional.linrec");h.require("dojox.lang.functional.lambda");h.require("dojox.lang.functional.util");(function(){var b=r.lang.functional,k=b.inlineLambda,h=["_r","_y.a"];b.linrec=function(a,e,f,g){var m,l,n,p,q={},c={},d=function(a){q[a]=1};"string"==typeof a?a=k(a,"_x",d):(m=b.lambda(a),a="_c.apply(this, _x)",c["_c\x3d_t.c"]=
1);"string"==typeof e?e=k(e,"_x",d):(l=b.lambda(e),e="_t.t.apply(this, _x)");"string"==typeof f?f=k(f,"_x",d):(n=b.lambda(f),f="_b.apply(this, _x)",c["_b\x3d_t.b"]=1);"string"==typeof g?g=k(g,h,d):(p=b.lambda(g),g="_a.call(this, _r, _y.a)",c["_a\x3d_t.a"]=1);d=b.keys(q);c=b.keys(c);a=new Function([],"var _x\x3darguments,_y,_r".concat(d.length?","+d.join(","):"",c.length?",_t\x3d_x.callee,"+c.join(","):l?",_t\x3d_x.callee":"",";for(;!",a,";_x\x3d",f,"){_y\x3d{p:_y,a:_x}}_r\x3d",e,";for(;_y;_y\x3d_y.p){_r\x3d",
g,"}return _r"));m&&(a.c=m);l&&(a.t=l);n&&(a.b=n);p&&(a.a=p);return a}})()});
//# sourceMappingURL=linrec.js.map