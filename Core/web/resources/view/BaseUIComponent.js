/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'jquery.notify'], 
		function($){
"use strict";

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
	
	this.clearErrors = this.clearErrors.bind(this);
	this.showError = this.showError.bind(this);
	this.showSuccess = this.showSuccess.bind(this);

	
};

BaseUIComponent.prototype.errorDiv = null;

/**
 * Update an image and its alt text
 */
BaseUIComponent.prototype.updateImage = function(imgNode, text, src){
	imgNode.attr('src', src);
	imgNode.attr('alt', text);
	imgNode.attr('title', text);
};

BaseUIComponent.prototype.showHelp = function(event){
	alert('Would show help for ' + event.data.helpId);
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
        message = "Mango API request was cancelled";
        color = "red";
        break;
    default:
        color = "red";
        break;
    }

    //if (jqXHR && jqXHR.url)
    //    message += ", url=" + jqXHR.url;
    if (mangoMessage)
        message += mangoMessage;
    else
    	message += 'unknown';
    
    if(message === 'Validation error')
    	this.showValidationErrors(jqXHR.responseJSON);

    if(this.errorDiv === null)
    	$.notify(message, {style: 'mango-error', position: 'top left', autoHide: true, autoHideDelay: 30000, clickToHide: true});
    else
    	this.errorDiv.notify(message, {style: 'mango-error'});
};
  
return BaseUIComponent;
});