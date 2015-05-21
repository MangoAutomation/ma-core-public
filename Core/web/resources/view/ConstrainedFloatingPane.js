/**
 * Used to display the help windows
 * 
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */
define(['dojo/_base/declare','dojox/layout/FloatingPane', 'dojo/_base/fx', 'dojo/_base/lang', 'dojo/dom-style', 'dojo/dnd/move', 'dojo/dom-geometry'], 
		function(declare, FloatingPane, fx, lang, domStyle, move, domGeom){
"use strict";

var ConstrainedFloatingPane = declare(FloatingPane, {
    
    show: function(/* Function? */callback){
        // summary:
        //      Show the FloatingPane
        var anim = fx.fadeIn({node:this.domNode, duration:this.duration,
            beforeBegin: lang.hitch(this,function(){
                this.domNode.style.display = "";
                this.domNode.style.visibility = "visible";
                if (this.dockTo && this.dockable) { this.dockTo._positionDock(null); }
                if (typeof callback == "function") { callback(); }
                this._isDocked = false;
                if (this._dockNode) {
                    this._dockNode.destroy();
                    this._dockNode = null;
                }
            })
        }).play();
        // use w / h from content box dimensions and x / y from position
        var contentBox = domGeom.getContentBox(this.domNode)
        var pos = domGeom.position(this.domNode, true);
        pos = lang.mixin(pos, {w: contentBox.w, h: contentBox.h});
        this.resize(pos);
        this._onShow(); // lazy load trigger
    }
    
});

return ConstrainedFloatingPane;

});