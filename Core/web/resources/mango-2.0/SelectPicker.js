/**
 * Select Input
 * getting Point Value Data.  
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 * @module {PointValueQueryInput} mango/PointValueQueryInput
 * @see PointValueQueryInput
 * @tutorial dataPointChart
 */
define(['jquery'], function($) {
"use strict";

SelectInput = function(divId, mixin, options){
    
    this.divId = divId;
    
    this.mixin = mixin;
    
    for(var i in options) {
        this[i] = options[i];
    }
    
    //Setup The Configuration
    this.configuration = $.extend(true, {}, this.getBaseConfiguration(), this.mixin);
    var self = this;
    this.configuration.onChange = function(){
        self.onChange($(this).val());
    };
};


SelectPickerConfiguration.prototype = {
        divId: null, //Id of div to place Picker
        mixin: null, //Configuration overload
        configuration: null, //Full mixed-in config
        selected: 0, //Index selected
        placeholder: null, //Optional placeholder text
        
        addItem: function(label, id, selected){
            var html = "<option></option>";
            $('#' + this.divId).append( $(html).text(label).val(id));
            if($('#' + this.divId).selectpicker !== undefined)
                $('#' + this.divId).selectpicker('refresh');
        },
        
        onChange: function(value){
            console.log(value);
        },
        
        create: function(){
            var self = this;
            var select = $('#' + this.divId);
            if($('#' + this.divId).selectpicker !== undefined)
                $('#' + this.divId).selectpicker();
            //Add the options
            for(var k in this.configuration.options){
                if(k == this.selected)
                    this.addItem(this.configuration.options[k].label, this.configuration.options[k].value, true);
                else
                    this.addItem(this.configuration.options[k].label, this.configuration.options[k].value, false);
                    
            }
            
            if(this.placeholder !== null)
                $('#' + this.divId).attr("placeholder", this.placeholder);

            //Add the onChange method
            select.change(self.configuration.onChange);
            if(this.configuration.options.length > 0){
                $('#' + this.divId).val(this.configuration.options[this.selected].value);
                if($('#' + this.divId).selectpicker !== undefined)
                    $('#' + this.divId).selectpicker('refresh');
            }
            
           
            
        },

        getBaseConfiguration: function(){
            return {
                options: [] //Array of {label, value}
            };
        }
        
};

return SelectPickerConfiguration;

}); // close define