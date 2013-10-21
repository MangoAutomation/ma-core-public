//>>built
define("xstyle/ext/event",["../main"],function(e){var d;return d={onProperty:function(b,a,c){e.addRenderer(b,a,c,function(a){d.on(a,b.slice(2),function(a){})})},on:document.addEventListener?function(b,a,c){b.addEventListener(a,c,!1)}:function(b,a,c){b.attachEvent(a,c)}}});
//@ sourceMappingURL=event.js.map