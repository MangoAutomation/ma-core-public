/**
 * Javascript Objects for the Displaying Data on HTML pages.  
 * 
 * 
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */

define(['jquery'], function($) {
"use strict";

/**
 * @constructs Configuration for Statistics
 * @param divId
 * @param dataProviderIds
 * @param mixin
 * @param options
 */
StartsAndRuntimeListConfiguration = function(divPrefix, dataProviderIds, mangoMixin, options){
    this.divPrefix = divPrefix;
    this.dataProviderIds = dataProviderIds;
    this.mangoMixin = mangoMixin;
    
    for(var i in options) {
        this[i] = options[i];
    }
    
};

/**
 * Starts and Runtime List Config
 */
StartsAndRuntimeListConfiguration.prototype = {
        divPrefix: null, //Div of chart
        
        mangoMixin: null, //Any Mango Serial Chart mixins
        
        configuration: null, //The full config with mixin
       
        dataProviderIds: null, //List of my data provider ids
        
        /**
         * Do the heavy lifting and create the item
         * @return AmChart created
         */
        createDisplay: function(){
            var stats = new MangoStartsAndRuntimeList(this.divPrefix, this.dataProviderIds);
            return $.extend(true, {}, stats, this.mangoMixin);
        }
};


/**
 * @constructs Statistics Starts and Runtimes List Display
 * @param dataProviderIds
 * @param options
 */
MangoStartsAndRuntimeList = function(divPrefix, dataProviderIds, options){
    
    this.divPrefix = divPrefix;
    this.dataProviderIds = dataProviderIds;
    
    for(var i in options) {
        this[i] = options[i];
    }
};

MangoStartsAndRuntimeList.prototype = {
        
        divPrefix: null,  //The prefix for all divs ie. myXidSum will contain the sum and prefix is myXid
        dataProviderIds: null,
        
        /**
         * Data Provider listener to clear data
         */
        onClear: function(){
            $("#" + this.divPrefix + "StartsAndRuntimes").text("");
            $("#" + this.divPrefix + "First").text("");
            $("#" + this.divPrefix + "Last").text("");
        },
        
        /**
         * Data Provider Listener
         * On Data Provider load we add new data
         */
        onLoad: function(data, dataPoint){
            if(data.hasData){
                //Starts and Runtimes Statistics
                var list = "";
                for(var i=0; i<data.startsAndRuntimes.length; i++){
                    list += "<tr><td>";
                    list += data.startsAndRuntimes[i].value;
                    list += "</td><td>" + data.startsAndRuntimes[i].runtime;
                    list += "</td><td>" + data.startsAndRuntimes[i].proportion;
                    list += "</td><td>" + data.startsAndRuntimes[i].starts;
                    list += "</td></tr>";
                }
                $("#" + this.divPrefix + "StartsAndRuntimes").html(list);
                $("#" + this.divPrefix + "First").text(this.renderPointValueTime(data.first));
                $("#" + this.divPrefix + "Last").text(this.renderPointValueTime(data.last));
            }
        },
        
        renderPointValueTime: function(pvt){
           return this.renderValue(pvt.value) + " @ " + this.renderTime(pvt.timestamp);  
        },
        
        renderValue: function(value){
        	if(typeof value === 'number')
        		return value.toFixed(2);
        	else
        		return value;
        },
        
        renderTime: function(timestamp){
           return new Date(timestamp);
        }
};

//make the related sub types accessible through the returned type
//alternatively could make only visible internally or put them in separate files
StartsAndRuntimeListConfiguration.MangoStartsAndRuntimeList = MangoStartsAndRuntimeList;

return StartsAndRuntimeListConfiguration;

}); // close define