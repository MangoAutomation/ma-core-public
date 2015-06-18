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
        'jquery.notify',
        'jquery-ui/jquery-ui'], 
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
 * Documentation Pane ID
 */
BaseUIComponent.prototype.documentationPanelId = 'mangoHelpModal';

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
	
	//TODO Un-comment when ready to use Bootstraps modal window
//	var help = $('#' + this.documentationPanelId);
//	if(!help.length){
//		help = $('<div id="' + this.documentationPaneId + '" class="modal">');
//		var content = $('<div class="modal-content">');
//		help.append(content);
//		var header = $('<div class="modal-header">');
//		header.append('<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>');
//		header.append('<h4 class="modal-title">Help Title</h4>');
//		content.append(header);
//		
//		var modalBody = $('<div class="modal-body">');
//		modalBody.append('<p>Help content here</p>');
//		content.append(modalBody);
//		
//		var modalFooter = $('<div class="modal-footer">');
//		content.append(modalFooter);
//		
//		$('body').append(help);
//	}
//	
//	help.draggable({
//		handle: ".modal-header"
//	});
//	
//	help.modal('show');

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
 * Handler for dstore errors, note that dstore does not currently parse the JSON response
 * so we parse it manually
 */
BaseUIComponent.prototype.dstoreErrorHandler = function(data) {
    var parsed = {};
    if (data.response && data.response.data) {
        try {
            parsed = $.parseJSON(data.response.data);
        } catch(e) { }
    }
    
    if (parsed.validationMessages && parsed.validationMessages.length) {
        this.showValidationErrors(parsed);
        return;
    }
    
    var message = data.message;
    // TODO maybe construct an object which can be passed to showError()
    // for consistency
    this.showGenericError(message);
};

/**
 * Display notification on element for every property which has validation errors
 * Return true if we have errors
 */
BaseUIComponent.prototype.showValidationErrors = function(vo){
	if(typeof vo.validationMessages === 'undefined')
		return false;
	if(vo.validationMessages.length === 0)
		return false;
	
	for (var i=0; i<vo.validationMessages.length; i++) {
		var msg = vo.validationMessages[i];
		var $element = this.elementForProperty(msg.property);
		if ($element.length) {
	        $element.notify(msg.message, {className: msg.level.toLowerCase(), position: 'right'});
		} else {
	        this.showGenericError(msg.message);
		}
	}
	
	return true;
};

BaseUIComponent.prototype.elementForProperty = function(propertyArray, $scope) {
    $scope = $scope || this.$scope;
    if (!$.isArray(propertyArray)) {
        propertyArray = propertyArray.split('.');
    }
    
    var prop = propertyArray.shift();
    var arrayMatch = prop.match(/(.+)\[(\d+)\]/);
    var arrayIndex;
    if (arrayMatch) {
        // redefine the property as being just the name of the array
        prop = arrayMatch[1];
        arrayIndex = arrayMatch[2];
    }
    
    // find the element with matching property, exclude nested properties
    var $element = $scope.find('[data-editor-property="' + prop + '"]')
        .not($scope.find('[data-editor-property] [data-editor-property]'));

    // if we didn't find the element and this is the last component of the property
    // then look for inputs too (using the name attribute)
    if (!propertyArray.length && !$element.length) {
        $element = $scope.find('[name="' + prop + '"]')
            .not($scope.find('[data-editor-property] [name]'));
    }
    
    if (arrayMatch) {
        // note that this will only work for dgrids using memory backed stores
        if ($element.hasClass('dgrid')) {
            // lookup grid from dijit registry
            var grid = registry.byNode($element[0]);
            var item = grid.collection.data[arrayIndex];
            $element = $(grid.row(item).element);
        } else {
            /* look for a nested element with data-editor-property set to the array index
             * e.g.
             * <ol data-editor-property="myArray">
             *    <li data-editor-property="0">List item 1</li>
             *    <li data-editor-property="1">List item 2</li>
             * </ol>
             */
            propertyArray.unshift(arrayIndex);
            
            // could also get the xth child?
            //$element = $element.children().eq(arrayMatch[2]);
        }
    }
    
    // non ideal fall-back, used on users page, keep here for compatibility
    if (!$element.length) {
        $element = $('#' + prop);
    }
    
    if (!propertyArray.length || !$element.length) {
        return $element;
    } else {
        return this.elementForProperty(propertyArray, $element);
    }
};

/**
 * Set inputs to the values from the item's properties
 * 
 * Note: does not support object and array properties, override .setProperty()
 */
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
    if ($element.hasClass('dijit')) {
        $element.filter('.dijit').each(function(i, node) {
            var dijit = registry.byNode(node);
            if (dijit) {
                dijit.set('value', value);
            }
        });
    } else {
        // not a dijit, use jquery to set input value
        if ($element.is('[type=checkbox]')) {
            $element.prop('checked', value).trigger('change');
        } else {
            $element.val(value).trigger('change');
        }
    }
};

/**
 * Retrieve values from inputs and set the properties on the item
 * 
 * Note: does not support object and array properties, override .getProperty()
 */
BaseUIComponent.prototype.getInputs = function(item) {
    var self = this;
    var $scope = this.$scope;
    $scope.find('[data-editor-property], [name]')
        // remove nested properties
        .not($scope.find('[data-editor-property] [data-editor-property]'))
        .not($scope.find('[data-editor-property] [name]'))
        .each(function(i, element) {
            var $element = $(element);
            var property = $element.attr('data-editor-property');
            if (!property) {
                property = $element.attr('name');
            }
                    
            var propertyValue = self.getProperty(item, property, $element);
            if (propertyValue !== undefined) {
                item[property] = propertyValue;
            }
        });
    return item;
};

BaseUIComponent.prototype.getProperty = function(item, property, $element) {
    if ($element.hasClass('dijit')) {
        var value;
        $element.filter('.dijit').each(function(i, node) {
            var dijit = registry.byNode(node);
            if (dijit) {
                value = dijit.get('value');
                return; // break
            }
        });
        return value;
    } else if ($element.is(':input')) {
        // not a dijit, use jquery to get input value
        if ($element.is('[type=checkbox]')) {
            return $element.prop('checked');
        } else {
            return $element.val();
        }
    }
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

/**
 * Displays error messages from jquery XHR requests, typically used with MangoAPI calls
 */
BaseUIComponent.prototype.showError = function(errorObject) {
    var logLevel = 'error';
    var message;
    switch(errorObject.type) {
    case 'loadNotNeeded':
    case 'tooMuchData':
    case 'noData':
        // dont display these messages
        return;
    case 'providerDisabled':
        message = errorObject.description;
        logLevel = 'warn';
        break;
    case 'jqXHR':
        if (errorObject.textStatus === 'abort') {
            message = 'Mango XHR request was cancelled';
            logLevel = 'warn';
        } else {
            message = "Mango XHR request failed";
            if (errorObject.textStatus)
                message += ", status=" + errorObject.textStatus;
            if (errorObject.errorThrown)
                message += ", error=" + errorObject.errorThrown;
            if (errorObject.mangoMessage)
                message += ", message=" + errorObject.mangoMessage;
        }
        
        if (errorObject.mangoMessage === 'Validation error') {
            this.showValidationErrors(errorObject.jqXHR.responseJSON);
            return;
        }
        
        message += ", url=" + errorObject.url;
        break;
    default:
        message = "Generic error: " + errorObject.type;
    }
    
    this.showGenericError(message, logLevel);
};

BaseUIComponent.prototype.showGenericError = function(message, level) {
    level = level || 'error';
    var style = 'mango-' + level;
    if (this.errorDiv) {
        this.errorDiv.notify(message, {style: style});
    } else {
        $.notify(message, {
            style: style,
            position: 'top center',
            autoHide: true,
            autoHideDelay: 30000,
            clickToHide: true
        });
    }
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
