/**
 * Manager for Data Provider Options
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @exports mango/RealtimeDataProvider
 * @module {ProviderOptionsManager} mango/ProviderOptionsManager
 * @tutorial allDataPointsChart
 */
define(['jquery', './api', 'moment-timezone', 'dstore/legacy/DstoreAdapter', 'dstore/Memory'],
function($, MangoAPI, moment, DstoreAdapter, Memory) {
"use strict";

//Note that the NONE rollup is now a value instead of ''

/**
 * Provider Options Manager simplifies the work of collecting inputs to trigger data point value events such as charting.
 * 
 * @constructs ProviderOptionsManager
 * @param {Object} options - options for manager
 * @tutorial allDataPointsChart
 * 
 */
function ProviderOptionsManager(options) {
    this.pickerChanged = this.pickerChanged.bind(this);
    
    this.providerOptions = {
        from: 0,
        to: 0,
        rollup: 'AVERAGE',
        timePeriodType: 'HOURS',
        timePeriods: 1
    };
    
    this.providers = [];
    
    $.extend(this, options);
    
    // call the setXXXPicker methods to register the on change events
    this.setTimePicker(this.timePicker);
    this.setRollupPicker(this.rollupPicker);
    this.setTimePeriodTypePicker(this.timePeriodTypePicker);
    this.setTimePeriodsPicker(this.timePeriodsPicker);
    
    // ensure user specified provider options take precedence
    $.extend(this.providerOptions, options.providerOptions);
}

ProviderOptionsManager.prototype.refreshOnChange = true;

/**
 * Used to set the default rollup period when a time preset is picked
 */
ProviderOptionsManager.prototype.graphType = 'line';

//Used to signal to disable the NONE rollup type
ProviderOptionsManager.prototype.allowNoneRollup = false;

ProviderOptionsManager.prototype.timePicker = null;
ProviderOptionsManager.prototype.rollupPicker = null;
ProviderOptionsManager.prototype.timePeriodTypePicker = null;
ProviderOptionsManager.prototype.timePeriodsPicker = null;

ProviderOptionsManager.prototype.errorFunction = function() {};
    
ProviderOptionsManager.prototype.refreshProviders = function(forceRefresh) {
    var self = this;
    $.each(this.providers, function(key, provider) {
        self.refreshProvider(provider, forceRefresh);
    });
};

ProviderOptionsManager.prototype.refreshProvider = function(provider, forceRefresh) {
    if (provider && provider.enabled) {
        // clone the options so providers can't modify them
        var options = $.extend({}, this.providerOptions);
        options.forceRefresh = forceRefresh || false;
        
        provider.load(options).fail(this.errorFunction);
    }
};

ProviderOptionsManager.prototype.clearDisplays = function() {
    $.each(this.providers, function(key, provider) {
        provider.clear();
    });
};

ProviderOptionsManager.prototype.addProvider = function(provider) {
    if (!provider)
        return;
    if ($.inArray(provider, this.providers) < 0)
        this.providers.push(provider);
};

/**
 * Finds an existing provider which matches an array of DataPointConfigurations
 * 
 * @param dataPointConfigurations - array of DataPointConfiguration
 * @returns a provider
 */
ProviderOptionsManager.prototype.findProvider = function(providerType, pointConfigs) {
    if (!$.isArray(pointConfigs))
        pointConfigs = [pointConfigs];
    
    for (var i = 0; i < this.providers.length; i++) {
        var provider = this.providers[i];
        if (provider.type !== providerType)
            continue;
        if (this.comparePointConfigs(pointConfigs, provider.pointConfigurations)) {
            return provider;
        }
    }
};

/**
 * Compares two DataPointConfiguration arrays based solely on point xid
 * 
 * @param array1
 * @param array2
 * @returns true if equivalent
 */
ProviderOptionsManager.prototype.comparePointConfigs = function(array1, array2) {
    if (array1.length !== array2.length)
        return false;
    
    for (var i = 0; i < array1.length; i++) {
        var config1 = array1[i];
        var foundConfig1 = false;
        for (var j = 0; j < array2.length; j++) {
            var config2 = array2[j];
            if (config1.point.xid == config2.point.xid) {
                foundConfig1 = true;
                break;
            }
        }
        if (!foundConfig1)
            return false;
    }
    return true;
};

ProviderOptionsManager.prototype.pickerChanged = function(event, data) {
    var triggerRefresh = true;
    
    if (event.target === this.timePicker) {
        if (!data.triggerRefresh)
            triggerRefresh = false;
        this.timePickerChanged(data.preset);
        //Could suggest rollup values here
    }
    
    this.loadFromPickers();
    
    if (triggerRefresh && this.refreshOnChange) {
        this.refreshProviders();
    }

    $(this).trigger("change", this.providerOptions);
};

ProviderOptionsManager.prototype.timePickerChanged = function(preset) {
    switch(preset) {
    case 'PREVIOUS_1_HR':
     this.providerOptions.timePeriodType = 'MINUTES';
     this.providerOptions.timePeriods = 1;
	 break;
    case 'PREVIOUS_6_HRS':
   	 if (this.graphType === 'line') {
         this.providerOptions.timePeriodType = 'MINUTES';
         this.providerOptions.timePeriods = 1;
     }
     else { // assume bar
         this.providerOptions.timePeriodType = 'MINUTES';
         this.providerOptions.timePeriods = 10;
     }
	 break;
    case 'PREVIOUS_12_HRS':
    case 'PREVIOUS_DAY':
    case 'YESTERDAY':
    case 'DAY_TO_DATE':
        if (this.graphType === 'line') {
            this.providerOptions.timePeriodType = 'MINUTES';
            this.providerOptions.timePeriods = 5;
        }
        else { // assume bar
            this.providerOptions.timePeriodType = 'HOURS';
            this.providerOptions.timePeriods = 1;
        }
        break;
    case 'PREVIOUS_WEEK':
    case 'LAST_WEEK':
    case 'WEEK_TO_DATE':
        if (this.graphType === 'line') {
            this.providerOptions.timePeriodType = 'HOURS';
            this.providerOptions.timePeriods = 1;
        }
        else { // assume bar
            this.providerOptions.timePeriodType = 'DAYS';
            this.providerOptions.timePeriods = 1;
        }
        break;
    case 'PREVIOUS_4WEEKS':
    case 'PREVIOUS_MONTH':
    case 'LAST_MONTH':
    case 'MONTH_TO_DATE':
        if (this.graphType === 'line') {
            this.providerOptions.timePeriodType = 'HOURS';
            this.providerOptions.timePeriods = 6;
        }
        else { // assume bar
            this.providerOptions.timePeriodType = 'DAYS';
            this.providerOptions.timePeriods = 1;
        }
        break;
    case 'PREVIOUS_YEAR':
    case 'LAST_YEAR':
    case 'YEAR_TO_DATE':
        if (this.graphType === 'line') {
            this.providerOptions.timePeriodType = 'DAYS';
            this.providerOptions.timePeriods = 1;
        }
        else { // assume bar
            this.providerOptions.timePeriodType = 'MONTHS';
            this.providerOptions.timePeriods = 1;
        }
        break;
    default:
        // dont change anything for custom time periods
        return;
    }
    
    this.setInputValue('timePeriods', this.providerOptions.timePeriods);
    this.setInputValue('timePeriodType', this.providerOptions.timePeriodType);
};

ProviderOptionsManager.prototype.quantizationPeriod = function() {
    return moment.duration(this.providerOptions.timePeriods,
            this.providerOptions.timePeriodType.toLowerCase());
};

ProviderOptionsManager.prototype.loadFromTimePicker = function() {
    if (this.timePicker) {
        var from = this.timePicker.from;
        var to = this.timePicker.to;
        
        if (to.isBefore(from))
            return;
        
        this.providerOptions.from = from.toDate();
        this.providerOptions.to = to.toDate();
        this.providerOptions.fromMoment = from;
        this.providerOptions.toMoment = to;
    }
};

ProviderOptionsManager.prototype.loadFromPickers = function() {
    this.loadFromTimePicker();
    
    var rollup = this.getInputValue('rollup');
    if (rollup || rollup === '') {
        this.providerOptions.rollup = rollup;
    }
    
    var timePeriodType = this.getInputValue('timePeriodType');
    if (timePeriodType) {
        this.providerOptions.timePeriodType = timePeriodType;
    }
    
    var timePeriods = parseInt(this.getInputValue('timePeriods'), 10);
    if (timePeriods) {
        this.providerOptions.timePeriods = timePeriods;
    }
    
    var noRollup = !this.providerOptions.rollup || this.providerOptions.rollup === 'NONE';
    this.disableInput('timePeriodType', noRollup);
    this.disableInput('timePeriods', noRollup);
};

/**
 * This is no longer used, we are now checking the count.
 * 
 * Dont allow tiny rollup periods for comparatively large date ranges
 * Also limit the use of no rollup to small periods
 */
ProviderOptionsManager.prototype.limitRollupPeriod = function() {
	var timePeriod = this.providerOptions.to - this.providerOptions.from;

    if ((!this.providerOptions.rollup || this.providerOptions.rollup === 'NONE') &&
            timePeriod > moment.duration(1, 'hours').asMilliseconds()) {
    	//We need to either choose a rollup that is available for all data types or track a data type here
    	this.allowNoneRollup = false;
    	this.providerOptions.rollup = 'FIRST'; 
    	this.setInputValue('rollup', this.providerOptions.rollup);
        this.providerOptions.timePeriods = 1;
        this.providerOptions.timePeriodType = 'MINUTES';
    }
    
    if (this.providerOptions.rollup !== 'NONE') {
        var timePeriods = this.providerOptions.timePeriods;
        var timePeriodType = this.providerOptions.timePeriodType.toLowerCase();
        var rollupPeriod = moment.duration(timePeriods, timePeriodType).asMilliseconds();
        
        if (timePeriod >  moment.duration(5, 'year').asMilliseconds()) {
            // more than 5 years
            if (rollupPeriod < moment.duration(1, 'months').asMilliseconds()) {
                this.providerOptions.timePeriods = 1;
                this.providerOptions.timePeriodType = 'MONTHS';
            }
        }
        else if (timePeriod >  moment.duration(1, 'years').asMilliseconds()) {
            // 1 years to 5 years: 5 years * 52 weeks = 260 periods
            if (rollupPeriod < moment.duration(1, 'weeks').asMilliseconds()) {
                this.providerOptions.timePeriods = 1;
                this.providerOptions.timePeriodType = 'WEEKS';
            }
        }
        else if (timePeriod >  moment.duration(31, 'days').asMilliseconds()) {
            // 31 days to 1 year: 12 months * 30 days = 360 periods
            if (rollupPeriod < moment.duration(1, 'days').asMilliseconds()) {
                this.providerOptions.timePeriods = 1;
                this.providerOptions.timePeriodType = 'DAYS';
            }
        }
        else if (timePeriod >  moment.duration(1, 'weeks').asMilliseconds()) {
            // 1 week to 1 month: 31 days * 24 hours = 744 periods
            if (rollupPeriod < moment.duration(1, 'hours').asMilliseconds()) {
                this.providerOptions.timePeriods = 1;
                this.providerOptions.timePeriodType = 'HOURS';
            }
        }
        else if (timePeriod >  moment.duration(2, 'days').asMilliseconds()) {
            // 2 days to 1 week: 7days * 24 hours * 60 minutes / 15 = 672 periods
            if (rollupPeriod < moment.duration(15, 'minutes').asMilliseconds()) {
                this.providerOptions.timePeriods = 15;
                this.providerOptions.timePeriodType = 'MINUTES';
            }
        }
        else if (timePeriod >  moment.duration(12, 'hours').asMilliseconds()) {
            // 12 hours to 2 day: 2 days * 24 hours * 60 minutes / 5 = 576 periods
            if (rollupPeriod < moment.duration(5, 'minutes').asMilliseconds()) {
                this.providerOptions.timePeriods = 5;
                this.providerOptions.timePeriodType = 'MINUTES';
            }
        }
        else if (timePeriod >  moment.duration(1, 'hours').asMilliseconds()) {
            // 1 hours to 12 hrs:  12 hours * 60 minutes / 1 =  720 periods
            if (rollupPeriod < moment.duration(1, 'minutes').asMilliseconds()) {
                this.providerOptions.timePeriods = 1;
                this.providerOptions.timePeriodType = 'MINUTES';
            }
        }else if (timePeriod > 0){
        	//For all time less than 1 hrs we can allow second level rollup
        	// 0 to 1 hours: 3600 periods yikes!
        	this.allowNoneRollup = true;
        	if (rollupPeriod < moment.duration(1, 'second').asMilliseconds()) {
                this.providerOptions.timePeriods = 1;
                this.providerOptions.timePeriodType = 'SECONDS';
            }
        }
    }

    this.setInputValue('timePeriods', this.providerOptions.timePeriods);
    this.setInputValue('timePeriodType', this.providerOptions.timePeriodType);
};

ProviderOptionsManager.prototype.setTimePicker = function(picker) {
    if (!picker) return;
    
    $(picker).on('change', this.pickerChanged);
    this.timePicker = picker;
    
    // load the default time set on the time picker
    this.loadFromTimePicker();
    
    // change the provider options to match the default preset
    this.timePickerChanged(this.timePicker.preset);
};

ProviderOptionsManager.prototype.setRollupPicker = function(picker) {
    this.setPicker('rollup', picker);
    ProviderOptionsManager.populatePicker(picker, 'rollup');
    this.setInputValue('rollup', this.providerOptions.rollup);
};

ProviderOptionsManager.prototype.setTimePeriodTypePicker = function(picker) {
    this.setPicker('timePeriodType', picker);
    ProviderOptionsManager.populatePicker(picker, 'timePeriodType');
    this.setInputValue('timePeriodType', this.providerOptions.timePeriodType);
};

ProviderOptionsManager.prototype.setTimePeriodsPicker = function(picker) {
    this.setPicker('timePeriods', picker);
    this.setInputValue('timePeriods', this.providerOptions.timePeriods);
};

ProviderOptionsManager.prototype.setPicker = function(pickerName, picker) {
    if (!picker) return;
    
    picker.on('change', this.pickerChanged);
    this[pickerName + 'Picker'] = picker;
};

ProviderOptionsManager.prototype.getInputValue = function(pickerName) {
    var picker = this[pickerName + 'Picker'];
    if (!picker)
        return undefined;
    
    if (picker instanceof $) {
        // jquery
        return picker.val();
    } else if (typeof picker.get == 'function') {
        // dojo
        return picker.get('value');
    }
    
    return null;
};

ProviderOptionsManager.prototype.setInputValue = function(pickerName, value) {
    var picker = this[pickerName + 'Picker'];
    if (!picker)
        return;
    
    if (picker instanceof $) {
        // jquery
        picker.val(value).trigger('change.select2');
    } else if (typeof picker.set == 'function') {
        // dojo
        picker.set('value', value, false);
    }
};

ProviderOptionsManager.prototype.disableInput = function(pickerName, disable) {
    var picker = this[pickerName + 'Picker'];
    if (!picker)
        return;
    
    if (picker instanceof $) {
        // jquery
        picker.attr('disabled', disable);
    } else if (typeof picker.set == 'function') {
        // dojo
        picker.set('disabled', disable);
    }
};

ProviderOptionsManager.populatePicker = function(picker, name) {
    if (!picker) return;
    var values = ProviderOptionsManager[name + 'Values'];
    if (!values) return;
    values = values.slice();
    
    if (picker instanceof $) {
        // jquery
        if (picker.children().length) return;
        
        for (var i = 0; i < values.length; i++) {
            var option = $('<option>');
            option.attr('value', values[i].id);
            option.text(values[i].name);
            option.data('valueObject', values[i]);
            picker.append(option);
        }
    } else if (picker.store && picker.store.data && !picker.store.data.length) {
        // dojo
        picker.set('store', new DstoreAdapter(new Memory({data: values}))); 
    }
};

ProviderOptionsManager.rollupValues = [
    //TODO change text to use: tr('datapointdetailsview.rollup.none')
    {id:'NONE', name: 'None', nonNumericSupport: true},
    {id:'AVERAGE', name: 'Average', nonNumericSupport: false},
    {id:'ACCUMULATOR', name: 'Accumulator', nonNumericSupport: false},
    {id:'COUNT', name: 'Count', nonNumericSupport: true},
    {id:'DELTA', name: 'Delta', nonNumericSupport: false},
    {id:'FFT', name: 'Dft', nonNumericSupport: false},
    {id:'INTEGRAL', name: 'Integral', nonNumericSupport: false},
    {id:'MAXIMUM', name: 'Maximum', nonNumericSupport: false},
    {id:'MINIMUM', name: 'Minimum', nonNumericSupport: false},
    {id:'SUM', name: 'Sum', nonNumericSupport: false},
    {id:'FIRST', name: 'First', nonNumericSupport: true},
    {id:'LAST', name: 'Last', nonNumericSupport: true}
];

ProviderOptionsManager.timePeriodTypeValues = [
    //TODO change text to use: tr('datapointdetailsview.rollup.none')
    {id:'SECONDS', name: 'Seconds'},
    {id:'MINUTES', name: 'Minutes'},
    {id:'HOURS', name: 'Hours'},
    {id:'DAYS', name: 'Days'},
    {id:'WEEKS', name: 'Weeks'},
    {id:'MONTHS', name: 'Months'},
    {id:'YEARS', name: 'Years'}
];

return ProviderOptionsManager;

}); // require
