/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery',
        'mango/api',
        './ConstrainedFloatingPane',
        'dojo/dom-style',
        'dojo/dom-geometry',
        'dijit/registry',
        'jquery.notify'], 
		function($, MangoAPI, ConstrainedFloatingPane, domStyle, domGeom, registry){
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
	    	'word-wrap' : 'break-word',
	    	'overflow-x' : 'auto',
	    	"padding": "8px 15px 8px 14px",
	    	"text-shadow": "0 1px 0 rgba(255, 255, 255, 0.5)",
	        "padding-left": "25px",
	        "background-repeat": "no-repeat",
	        "background-position": "3px 7px",
	        "background-image": "url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAtRJREFUeNqkVc1u00AQHq+dOD+0poIQfkIjalW0SEGqRMuRnHos3DjwAH0ArlyQeANOOSMeAA5VjyBxKBQhgSpVUKKQNGloFdw4cWw2jtfMOna6JOUArDTazXi/b3dm55socPqQhFka++aHBsI8GsopRJERNFlY88FCEk9Yiwf8RhgRyaHFQpPHCDmZG5oX2ui2yilkcTT1AcDsbYC1NMAyOi7zTX2Agx7A9luAl88BauiiQ/cJaZQfIpAlngDcvZZMrl8vFPK5+XktrWlx3/ehZ5r9+t6e+WVnp1pxnNIjgBe4/6dAysQc8dsmHwPcW9C0h3fW1hans1ltwJhy0GxK7XZbUlMp5Ww2eyan6+ft/f2FAqXGK4CvQk5HueFz7D6GOZtIrK+srupdx1GRBBqNBtzc2AiMr7nPplRdKhb1q6q6zjFhrklEFOUutoQ50xcX86ZlqaZpQrfbBdu2R6/G19zX6XSgh6RX5ubyHCM8nqSID6ICrGiZjGYYxojEsiw4PDwMSL5VKsC8Yf4VRYFzMzMaxwjlJSlCyAQ9l0CW44PBADzXhe7xMdi9HtTrdYjFYkDQL0cn4Xdq2/EAE+InCnvADTf2eah4Sx9vExQjkqXT6aAERICMewd/UAp/IeYANM2joxt+q5VI+ieq2i0Wg3l6DNzHwTERPgo1ko7XBXj3vdlsT2F+UuhIhYkp7u7CarkcrFOCtR3H5JiwbAIeImjT/YQKKBtGjRFCU5IUgFRe7fF4cCNVIPMYo3VKqxwjyNAXNepuopyqnld602qVsfRpEkkz+GFL1wPj6ySXBpJtWVa5xlhpcyhBNwpZHmtX8AGgfIExo0ZpzkWVTBGiXCSEaHh62/PoR0p/vHaczxXGnj4bSo+G78lELU80h1uogBwWLf5YlsPmgDEd4M236xjm+8nm4IuE/9u+/PH2JXZfbwz4zw1WbO+SQPpXfwG/BBgAhCNZiSb/pOQAAAAASUVORK5CYII=)"
	    }
	  }
	});

function BaseUIComponent(options) {
    $.extend(this, options);
    
	this.clearErrors = this.clearErrors.bind(this);
	this.showError = this.showError.bind(this);
	this.showSuccess = this.showSuccess.bind(this);
	this.getCurrentUser = this.getCurrentUser.bind(this);
	
	var self = this;
	this.setupTranslations().then(function() {
	    $(document).ready(self.documentReady.bind(self));
	}, this.showError);
}

/**
 * Optionally place errors into a div instead of via notifications
 */
BaseUIComponent.prototype.errorDiv = null;

/**
 * Mango Rest API Util
 */
BaseUIComponent.prototype.api = MangoAPI.defaultApi;

/**
 * Mango Translation utility
 */
BaseUIComponent.prototype.tr = null; 

/**
 * Translation name spaces to be setup
 */
BaseUIComponent.prototype.translationNamespaces = ['common', 'header', 'js.help']; 

/**
 * jQuery selection which will be searched for inputs when
 * calling getInputs(), setInputs() and showValidationErrors(),
 * also used to find help links etc
 */
BaseUIComponent.prototype.$scope = $(document.body);

/**
 * Maps property names to elements
 */
BaseUIComponent.prototype.propertyMap = {};

/**
 * Get the current user
 * @return promise with user as data
 */
BaseUIComponent.prototype.getCurrentUser = function(){
	return this.api.getCurrentUser();
};

/**
 * Load in the translation namespaces
 * @return promise when ready
 */
BaseUIComponent.prototype.setupTranslations = function(namespaces){
	
	if(typeof namespaces != 'undefined')
		this.translationNamespaces = this.translationNamespaces.concat(namespaces);
	var deferred = $.Deferred();
	var self = this;
	//Load in the required data
	this.api.setupGlobalize.apply(this.api, this.translationNamespaces).done(function(Globalize){
		self.tr = Globalize.tr.bind(Globalize);
		deferred.resolve();
	}).fail(this.showError);
	
	return deferred.promise();
};

/**
 * Update an image and its alt text
 */
BaseUIComponent.prototype.updateImage = function(imgNode, text, src){
	imgNode.attr('src', src);
	imgNode.attr('alt', text);
	imgNode.attr('title', text);
};

BaseUIComponent.prototype.documentationPaneId = 'documentationPane';

BaseUIComponent.prototype.setupHelpDialog = function() {
    var fp = dijit.byId(this.documentationPaneId);
    if (fp) return fp;
    
    var div = dojo.doc.createElement('div');
    dojo.body().appendChild(div);
    fp = new ConstrainedFloatingPane({
        id: this.documentationPaneId,
        title: this.tr('js.help.loading'), 
        closeable: true,
        dockable: false,
        resizable: true,
        style: "position: absolute; zIndex: 980; padding: 2px; left:10; top:10; width: 400px; height: 300px"
    }, div);
    
    fp.startup();
    
    return fp;
};

/**
 * Show the Help via an onclick event
 * Ensure the id is passed in as event.data.helpId
 */
BaseUIComponent.prototype.showHelp = function(event){
    var fp = dijit.byId(this.documentationPaneId);
    if (!fp) {
        fp = this.setupHelpDialog();
        
        // Only set position on the first load
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
            left:left +"px",
            top:top +"px"
        });
    }
    
    var self = this;
    //Now Load in the help
    var helpId = (event.data && event.data.helpId) || $(event.target).data('help-id');
    this.api.getHelp(helpId).done(function(result){
        
    	fp.set('title', result.title);
    	var content = $('<div/>');
    	content.html(result.content);
        if (result.relatedList && result.relatedList.length > 0) {
        	var related = $('<p><b>' + self.tr('js.help.related') + '</b><br/></p>');
        	related.html();
            for (var i=0; i<result.relatedList.length; i++){
            	var helpId = result.relatedList[i].id;
            	var link = $('<a class="ptr">' + result.relatedList[i].title + "</a></br>" );
            	link.on('click', {helpId: helpId}, self.showHelp.bind(self));
            	related.append(link);
            }
            content.append(related);
        }
        if (result.lastUpdated){
        	var lastUpdated = $('<p>' + self.tr('js.help.lastUpdated') + ':' + result.lastUpdated + '</p>');
            content.append(lastUpdated);
        }
        
        var spacer = $('<div style="height:13px"></div>');
        content.append(spacer);
        fp.set('content', content);
        fp.show();
    	
    }).fail(this.showError);
};

BaseUIComponent.prototype.showSuccess = function(message){
	//var msgNode = $('<div style="color:green">').html(message);
	if(this.errorDiv === null)
		$.notify(message, {className: 'success', position: 'top center'});
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
		var $element = elementForProperty(msg.property);
		$element.notify(msg.message, {className: msg.level.toLowerCase(), position: 'right'});
	}
	
	return true;
};

BaseUIComponent.prototype.elementForProperty = function(property) {
    var $element = this.$scope.find('[data-editor-property="' + property + '"]');
    if (!$element.length)
        $element = this.$scope.find('[name="' + property + '"]');
    if (!$element.length)
        $element = $('#' + property);
    return $element;
};

BaseUIComponent.prototype.setInputs = function(item) {
    // clear inputs
    this.$scope.find('input, select, textarea').val('');
    
    // set the inputs
    for (var property in item) {
        var $element = this.elementForProperty(property);
        this.setProperty(item, property, $element, item[property]);
    }
};

BaseUIComponent.prototype.setProperty = function(item, property, $element, value) {
    $element.val(value);
};

BaseUIComponent.prototype.getInputs = function(item) {
    var self = this;
    this.$scope.find('[data-editor-property], [name]').each(function(i, element) {
        var $element = $(element);
        var property = $element.attr('data-editor-property');
        if (!property) {
            property = $element.attr('name');
        }
        item[property] = self.getProperty(item, property, $element);
    });
    return item;
};

BaseUIComponent.prototype.getProperty = function(item, property, $element) {
    return $element.val();
};

BaseUIComponent.prototype.clearErrors = function(){
	//Not using yet, since the notify will auto clear
};

/**
 * Show a generic message
 * @param level {String} - one of success, error, warn, info
 */
BaseUIComponent.prototype.showMessage = function(message, level){
	if(this.errorDiv === null){
		if(level === 'error')
			$.notify(message, {style: 'mango-error', position: 'top center'});
		else
			$.notify(message, {className: level, position: 'top center'});
	}else
		this.errorDiv.notify(message, level);
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
    //case 'error':
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
    	$.notify(message, {style: 'mango-error', position: 'top center', autoHide: true, autoHideDelay: 30000, clickToHide: true});
    else
    	this.errorDiv.notify(message, {style: 'mango-error'});
};

BaseUIComponent.prototype.escapeQuotes = function(str) {
    if (!str)
        return "";
    return str.replace(/\'/g,"\\'");
};

BaseUIComponent.prototype.escapeDQuotes = function(str) {
    if (!str)
        return "";
    return str.replace(/\"/g,"\\\"");
};

BaseUIComponent.prototype.encodeQuotes = function(str) {
    if (!str)
        return "";
    return str.replace(/\'/g,"%27").replace(/\"/g,"%22");
};

BaseUIComponent.prototype.encodeHtml = function(str) {
    if (!str)
        return "";
    str = str.replace(/&/g,"&amp;");
    return str.replace(/</g,"&lt;");
};

BaseUIComponent.prototype.documentReady = function() {
    this.dgridTabResize();
    this.setupConfirmDialog();
    this.$scope.find('.mango-help').on('click', this.showHelp.bind(this));
};

BaseUIComponent.prototype.setupConfirmDialog = function() {
    if ($('#confirmDialog').length) return;
    
    var $confirmDialog = $('<div id="confirmDialog" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true"><div class="modal-dialog modal-sm"><div class="modal-content"></div></div></div>');
    var $content = $confirmDialog.find('.modal-content');
    var $header = $('<div class="modal-header"></div>').appendTo($content);
    var $body = $('<div class="modal-body"></div>').appendTo($content);
    var $footer = $('<div class="modal-footer"></div>').appendTo($content);
    
    var $close = $('<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span></button>');
    $close.attr('aria-label', this.tr('common.cancel'));
    $close.appendTo($header);
    
    $header.append('<h4 class="modal-title"></h4>');
    
    var $cancel = $('<button type="button" class="btn btn-default" data-dismiss="modal"></button>');
    $cancel.text(this.tr('common.cancel')).appendTo($footer);
    var $ok = $('<button type="button" class="btn btn-primary" data-dismiss="modal"></button>');
    $ok.text(this.tr('common.ok')).appendTo($footer);
    
    $(document.body).append($confirmDialog);
};

BaseUIComponent.prototype.confirm = function(title, message) {
    var $confirm = $('#confirmDialog');
    $confirm.find('.modal-title').text(title);
    $confirm.find('.modal-body').text(message);
    
    var deferred = $.Deferred();
    
    var $okButton = $confirm.find('.btn-primary');
    $okButton.off('click');
    $okButton.on('click', deferred.resolve.bind(deferred));
    
    $confirm.off('hide.bs.modal');
    $confirm.on('hide.bs.modal', deferred.reject.bind(deferred));
    
    $confirm.modal('show');
    
    return deferred.promise();
};

/**
 * Finds dgrid components within $scope and calls resize on them
 */
BaseUIComponent.dgridResize = function($scope) {
    $scope.find('.dgrid').each(function(i, node) {
        // this requires the grid to be mixed in with 'dgrid/extensions/DijitRegistry'
        var grid = registry.byNode(node);
        // need to resize in case the window was resized
        grid.resize(); 
    });
};

/**
 * Finds dgrid components within $scope and calls resize on them
 */
BaseUIComponent.prototype.dgridResize = function($scope) {
    $scope = $scope || this.$scope;
    BaseUIComponent.dgridResize($scope);
};

/**
 * ensure dgrid components inside bootstrap tabs are loaded correctly when tab is shown
 */
BaseUIComponent.prototype.dgridTabResize = function() {
    var self = this;
    $('a[data-toggle="tab"]').on('shown.bs.tab', function (event) {
        var href = $(event.target).attr('href');
        BaseUIComponent.dgridResize($(href));
    });
};

return BaseUIComponent;
});