/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'mango/api', 'view/ConstrainedFloatingPane', 'dojo/dom-style', 'dojo/dom-geometry', 'jquery.notify'], 
		function($, MangoAPI, ConstrainedFloatingPane, domStyle, domGeom){
"use strict";
/* Style for Notification using multi-lines */
$.notify.addStyle('mango-error', {
	  html: "<div data-notify-text></div>",
	  classes: {
	    base: {
	    	'color': '#B94A48',
	    	'background-color': '#F2DEDE',
	    	'border-color': '#EED3D7',
	    	'white-space': 'normal',
	    	'border-radius': '5px',
	    	"font-weight": "bold",
	    	"padding": "8px 15px 8px 14px",
	    	"text-shadow": "0 1px 0 rgba(255, 255, 255, 0.5)"
	    }
	  }
	});

function BaseUIComponent(){
	
	this.api = MangoAPI.defaultApi;
	
	this.clearErrors = this.clearErrors.bind(this);
	this.showError = this.showError.bind(this);
	this.showSuccess = this.showSuccess.bind(this);

};

BaseUIComponent.prototype.errorDiv = null;
/**
 * Mango Rest API Util
 */
BaseUIComponent.prototype.api = null; 

/**
 * Update an image and its alt text
 */
BaseUIComponent.prototype.updateImage = function(imgNode, text, src){
	imgNode.attr('src', src);
	imgNode.attr('alt', text);
	imgNode.attr('title', text);
};

/**
 * Show the Help via an onclick event
 * Ensure the id is passed in as event.data.helpId
 */
BaseUIComponent.prototype.showHelp = function(event){
	
	var fpId = "documentationPane";
    var fp = dijit.byId(fpId);
    if (!fp) {
        var div = dojo.doc.createElement("div");
        dojo.body().appendChild(div);
        fp = new ConstrainedFloatingPane({
            id: fpId,
            title: tr("js.help.loading"), 
            closeable: true,
            dockable: false,
            resizable: true,
            style: "position: absolute; zIndex: 980; padding: 2px; left:10; top:10;"
        }, div);
        
        fp.startup();
    }
    
    var top, left;
    if (event.target) {
        var position = domGeom.position(event.target, false);
        left = position.x + position.w;
        top = position.y + position.h;
    }
    else {
        left = 10;
        top = 10;
    }

    var scroll = domGeom.docScroll();
    left += scroll.x;
    top += scroll.y;
    
    domStyle.set(fp.domNode, {
        width:"400px",
        height:"300px",
        left:left +"px",
        top:top +"px"
    });

    
    //Now Load in the help
    this.api.getHelp(event.data.helpId).done(function(result){
    	
    	fp.set('title', result.title);
        var content = result.content;
        if (result.relatedList && result.relatedList.length > 0) {
            content += "<p><b>"+ tr("js.help.related") +"</b><br/>";
            for (var i=0; i<result.relatedList.length; i++)
                content += "<a class='ptr' onclick='helpImpl(\""+ result.relatedList[i].id +"\");'>"+
                        result.relatedList[i].title +"</a><br/>";
            content += "</p>";
        }
        if (result.lastUpdated)
            content += "<p>"+ tr('js.help.lastUpdated') +': '+ result.lastUpdated +"</p>";
        content = "<div>"+ content +"</div><div style='height:13px'></div>";
        fp.set('content', content);
        fp.show();
    	
    }).fail(this.showError);
};

BaseUIComponent.prototype.showSuccess = function(message){
	//var msgNode = $('<div style="color:green">').html(message);
	if(this.errorDiv == null)
		$.notify(message, {className: 'success', position: 'top left'});
	else
		this.errorDiv.notify(message, 'success');
};

/**
 * Return true if we have errors
 */
BaseUIComponent.prototype.showValidationErrors = function(vo){
	if(typeof vo.validationMessages === 'undefined')
		return false;
	if(vo.validationMessages.length === 0)
		return false;
	
	for(var i=0; i<vo.validationMessages.length; i++){
		var msg = vo.validationMessages[i];
		$('#' + msg.property).notify(msg.message, {className: msg.level.toLowerCase(), position: 'right'});
	}
	
	return true;
};

BaseUIComponent.prototype.clearErrors = function(){
	//Not using yet, since the notify will auto clear
};

BaseUIComponent.prototype.showError = function(jqXHR, textStatus, error, mangoMessage){

	var logLevel, message='', color;
    switch(textStatus) {
    case 'notNeeded':
        // request cancelled as it wasn't needed
        return;
    case 'abort':
        message = "Mango API request was cancelled. ";
        color = "red";
        break;
    case 'parsererror':
    	color = "red";
    	message = "Response Parser Error. ";
    	break;
    case 'error':
    default:
        color = "red";
        break;
    }

    //if (jqXHR && jqXHR.url)
    //    message += ", url=" + jqXHR.url;
    if (mangoMessage)
        message += mangoMessage;
    else{
    	if((jqXHR)&&(jqXHR.statusText))
    		message += jqXHR.statusText;
    	else
    		message += 'unknown';
    }
    
    if(message === 'Validation error')
    	this.showValidationErrors(jqXHR.responseJSON);

    if(this.errorDiv === null)
    	$.notify(message, {style: 'mango-error', position: 'top left', autoHide: true, autoHideDelay: 30000, clickToHide: true});
    else
    	this.errorDiv.notify(message, {style: 'mango-error'});
};



BaseUIComponent.prototype.escapeQuotes = function(str) {
    if (!str)
        return "";
    return str.replace(/\'/g,"\\'");
}

BaseUIComponent.prototype.escapeDQuotes = function(str) {
    if (!str)
        return "";
    return str.replace(/\"/g,"\\\"");
}

BaseUIComponent.prototype.encodeQuotes = function(str) {
    if (!str)
        return "";
    return str.replace(/\'/g,"%27").replace(/\"/g,"%22");
}

BaseUIComponent.prototype.encodeHtml = function(str) {
    if (!str)
        return "";
    str = str.replace(/&/g,"&amp;");
    return str.replace(/</g,"&lt;");
}


return BaseUIComponent;
});