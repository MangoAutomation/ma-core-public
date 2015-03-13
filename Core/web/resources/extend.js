(function(global, factory) {
    if (typeof define === "function" && define.amd) define(factory);
    else if (typeof module === "object") module.exports = factory();
    else global.extend = factory();
}(this, function() {
    "use strict";
    
    // use Object.create if available, otherwise use shim
    var create = Object.create ? Object.create : function(proto) {
        function Type() {}
        Type.prototype = proto;
        return new Type();
    };
    
    var Base = function() {};
    Base.extend = function(parent, properties) {
        if (arguments.length < 2) {
            properties = arguments[0];
            parent = typeof this === 'function' ? this : Base;
        }
        
        // setup new prototype
        var prototype = create(parent.prototype);
        
        // copy supplied properties into the prototype
        for (var key in properties)
            prototype[key] = properties[key];
        
        // call parent constructor if new class doesn't have its own
        var Extended = prototype.hasOwnProperty('constructor') ?  prototype.constructor :
            function() { return prototype.constructor.apply(this, arguments); };
        Extended.prototype = prototype;
        Extended.extend = Base.extend; // give the subclass an extend method
        return Extended;
    };
    
    return Base.extend;
}));