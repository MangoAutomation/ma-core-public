/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent'], 
		function($, BaseUIComponent){
"use strict";

function ToolbarUtilities(){
	BaseUIComponent.apply(this, arguments);
}

ToolbarUtilities.prototype = Object.create(BaseUIComponent.prototype);

/**
 * Active Events
 */
ToolbarUtilities.prototype.eventsActiveSummary = null;

/**
 * Label text for None Level Events
 */
ToolbarUtilities.prototype.noneLabel = null;

/**
 * Label text for Info Events
 */
ToolbarUtilities.prototype.infoLabel = null;

/**
 * Label text for Urgent Events
 */
ToolbarUtilities.prototype.UrgetLabel = null;

/**
 * Label text for Critical Events
 */
ToolbarUtilities.prototype.criticalLabel = null;

/**
 * Label text for Life Safety Events
 */
ToolbarUtilities.prototype.lifeSafetyLabel = null;


/**
 * Setup the events view
 */
ToolbarUtilities.prototype.setupEventsSummary = function(activeEvents) {
	
	this.eventsActiveSummary = activeEvents;
	
	//Setup the labels
	this.lifeSafetyLabel = this.tr('common.alarmLevel.lifeSafety');
	this.criticalLabel = this.tr('common.alarmLevel.critical');
	this.urgentLabel = this.tr('common.alarmLevel.urgent');
	this.infoLabel = this.tr('common.alarmLevel.info');
	this.noneLabel = this.tr('common.alarmLevel.none');
	
	var self = this;
    this.api.registerForAlarmEvents(
            ['ACKNOWLEDGED', 'RAISED', 'RETURN_TO_NORMAL', 'DEACTIVATED'],
            ['LIFE_SAFETY', 'CRITICAL', 'URGENT', 'INFORMATION', 'NONE'],
    function(data) {
        // onMessage
        //TODO Check ERROR Status of data
        //Find the level we are working with
        var level = self.getEventLevel(data.payload.event);
        // TODO increment and decrement the unsilencedCount based on the events received
        switch(data.payload.type){
        case 'RAISED':
        	level.mostRecentUnsilenced = data.payload.event;
        	level.unsilencedCount++;
        break;
        case 'RETURN_TO_NORMAL':
        	if(data.payload.event.acknowledged)
        		level.unsilencedCount--;
        break;
        case 'DEACTIVATED':
        	if(data.payload.event.acknowledged)
        		level.unsilencedCount--;
        break;
        case 'ACKNOWLEDGED':
        	if(data.payload.event.acknowledged)
        		level.unsilencedCount--;
        break;
        }
        self.renderEventLevel(level);
    }, function(){}, function(){}, function(){});
    
    for (var i = 0; i < this.eventsActiveSummary.length; i++) {
        this.renderEventLevel(this.eventsActiveSummary[i]);
    }
};

/**
 * 
 */
ToolbarUtilities.prototype.getEventLevel = function(event){
	for (var i = 0; i < this.eventsActiveSummary.length; i++) {
		if(this.eventsActiveSummary[i].level === event.alarmLevel)
			return this.eventsActiveSummary[i];
	}
	return null;
};

/**
 * 
 */
ToolbarUtilities.prototype.renderEventLevel = function(level) {
    var event = level.mostRecentUnsilenced;
    var levelName = level.level;
    var cssClass = levelName.replace('_', '-').toLowerCase() + '-event';
    var $div = $('.event-summary .level-summary.' + cssClass);
    
    $div.empty();
    
    if (level.unsilencedCount) {
        this.setLevelProps(level);
        
        var $a = $('<a>')
            .prop('href', '/events.shtm?level=' + level.urlParameter)
            .appendTo($div);
        
        var $img = $('<img>')
            .prop('src', '/images/flag_' + level.flagColour + '.png')
            .appendTo($a);
        
        if (level.unsilencedCount === 1) {
            // display single event message and ack button
            $a.append(level.translatedName + ': ' + event.message);
            $div.append(acknowledgeEventTick(event));
        } else  {
            // display link and count
            $a.append(this.tr('common.alarmLevel.numOfEvents', level.unsilencedCount, level.translatedName));
        }
        $div.show();
    } else {
        // hide
        $div.hide();
    }
};

/**
 * 
 */
ToolbarUtilities.prototype.setLevelProps = function(level) {
    switch (level.level) {
    case 'LIFE_SAFETY':
        level.translatedName = this.lifeSafetyLabel;
        level.flagColour = 'red';
        level.urlParameter = 'lifeSafety';
        return;
    case 'CRITICAL':
        level.translatedName = this.criticalLabel;
        level.flagColour = 'orange';
        level.urlParameter = 'critical';
        return;
    case 'URGENT':
        level.translatedName = this.urgentLabel;
        level.flagColour = 'yellow';
        level.urlParameter = 'urgent';
        return;
    case 'INFORMATION':
        level.translatedName = this.infoLabel;
        level.flagColour = 'blue';
        level.urlParameter = 'info';
        return;
    case 'NONE':
        level.translatedName = this.noneLabel;
        level.flagColour = 'green';
        level.urlParameter = 'none';
        return;
    }
};


return ToolbarUtilities;

});