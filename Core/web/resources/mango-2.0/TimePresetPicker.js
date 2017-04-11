/**
 * Inputs for To/From Dates and various Preset intervals
 * 
 * @copyright 2015 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 * @module {TimePresetPicker} mango/TimePresetPicker
 * @see TimePresetPicker
 * @tutorial dataPointChart
 */
define(['jquery', 'moment-timezone', 'dstore/legacy/DstoreAdapter',
        'dstore/Memory', 'jquery-ui/jquery.datetimepicker'], 
		function($, moment, DstoreAdapter, Memory) {

/*
 * The time period options and values are taken from the Java class
 * com.serotonin.m2m2.TimePeriodDescriptor. Ensure that they match. 
 */

/**
 * @constructs TimePresetPicker
 * @param {Object} options - options for picker
 */
TimePresetPicker = function(options) {
    this.presetPickerChanged = this.presetPickerChanged.bind(this);
    this.fromToPickerChanged = this.fromToPickerChanged.bind(this);
    
    $.extend(this, options);
    
    this.setPresetPicker(this.presetPicker);
    this.setFromPicker(this.fromPicker);
    this.setToPicker(this.toPicker);
    
    this.setDefaultPreset(false);
};

/**
 * Preset Date Range Picker
 * @type {?Object}
 * @default null
 */
TimePresetPicker.prototype.presetPicker = null;
/**
 * @type {?Object}
 * @default null
 */
TimePresetPicker.prototype.fromPicker = null;
/**
 * @type {?Object}
 * @default null
 */
TimePresetPicker.prototype.toPicker = null;
/**
 * Current preset
 * @type {string}
 * @default null
 */
TimePresetPicker.prototype.preset = null;
/**
 * Current from date
 * @type {date}
 * @default null
 */
TimePresetPicker.prototype.from = null;
/**
 * Current to date
 * @type {date}
 * @default null
 */
TimePresetPicker.prototype.to = null;
/**
 * Format
 * @type {string}
 */
TimePresetPicker.prototype.format = 'lll';
/**
 * Time Format
 * @type {string}
 */
TimePresetPicker.prototype.formatTime = 'LT';
/**
 * Date Format
 * @type {string}
 */
TimePresetPicker.prototype.formatDate = 'l';
/**
 * Default Period
 * @type {string}
 */
TimePresetPicker.prototype.defaultPeriod = 'PREVIOUS_DAY';

/**
 * Set Preset picker
 * @param {Object} picker
 */
TimePresetPicker.prototype.setPresetPicker = function(picker) {
    if (!picker) return;
    
    picker.on('change', this.presetPickerChanged);
    this.presetPicker = picker;
    TimePresetPicker.populatePresetPicker(this.presetPicker);
};

/**
 * Helper to Set to/from picker
 * @param {Object} picker
 * @param {string} pickerName
 */
TimePresetPicker.prototype.setToFromPicker = function(pickerName, picker) {
    if (!picker) return;
    
    picker.datetimepicker({
        format: this.format,
        formatTime: this.formatTime,
        formatDate: this.formatDate
    });
    picker.on('change', this.fromToPickerChanged);
    this[pickerName + 'Picker'] = picker;
};

/**
 * Set from picker
 * @param {Object} picker
 */
TimePresetPicker.prototype.setFromPicker = function(picker) {
    this.setToFromPicker('from', picker);
};

/**
 * Set to picker
 * @param {Object} picker
 */
TimePresetPicker.prototype.setToPicker = function(picker) {
    this.setToFromPicker('to', picker);
};

/**
 * Get from date
 * @returns {date}
 */
TimePresetPicker.prototype.fromDate = function() {
    return this.from.toDate();
};

/**
 * Get to date
 * @returns {date}
 */
TimePresetPicker.prototype.toDate = function() {
    return this.to.toDate();
};

/**
 * Get current preset
 * @return {string}
 */
TimePresetPicker.prototype.currentPreset = function() {
    return this.preset;
};

/**
 * Hourly difference in dates
 * @returns {boolean}
 */
TimePresetPicker.prototype.hours = function() {
    return this.to.diff(this.from, 'hours', true);
};

/**
 * From/To Picker has changed
 */
TimePresetPicker.prototype.fromToPickerChanged = function(event) {
    var zone = moment.defaultZone && moment.defaultZone.name;
    var from = zone ? moment.tz(this.getInputValue('from'), this.format, zone) :
        moment(this.getInputValue('from'), this.format);
    var to = zone ? moment.tz(this.getInputValue('to'), this.format, zone) :
        moment(this.getInputValue('to'), this.format);
    
    if (from.isValid())
        this.from = from;
    if (to.isValid())
        this.to = to;
    
    this.setPreset('FIXED_TO_FIXED');
};

/**
 * Preset Picker Changed
 * @param event
 */
TimePresetPicker.prototype.presetPickerChanged = function(event) {
    var preset = this.getInputValue('preset');
    this.setPreset(preset);
};

/**
 * Set the default preset
 * @param {boolean} triggerRefresh
 */
TimePresetPicker.prototype.setDefaultPreset = function(triggerRefresh) {
    if (typeof triggerRefresh === 'undefined')
        triggerRefresh = true;
    this.setPreset(this.defaultPeriod, triggerRefresh);
};

/**
 * Set preset value
 * @param {string} preset
 * @param {boolean} triggerRefresh 
 */
TimePresetPicker.prototype.setPreset = function(preset, triggerRefresh) {
    if (typeof triggerRefresh === 'undefined')
        triggerRefresh = true;
    
    this.preset = preset;
    this.setInputValue('preset', preset);
    
    var period = TimePresetPicker.calculatePeriod(preset);
    if (period.from) {
        this.from = period.from;
        this.setInputValue('from', period.from.format(this.format));
    }
    if (period.to) {
        this.to = period.to;
        this.setInputValue('to', period.to.format(this.format));
    }
    
    $(this).trigger("change", {
    	preset: this.preset,
    	from: this.from,
    	to: this.to,
    	triggerRefresh: triggerRefresh
	});
};

TimePresetPicker.prototype.getInputValue = function(pickerName) {
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

TimePresetPicker.prototype.setInputValue = function(pickerName, value) {
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

/**
 * Presets for Custom Time Periods
 * @type {Object} 
 */
TimePresetPicker.presetValues = [
    {
    	id: "PREVIOUS_1_HR",
    	value: 10,
    	name: "Previous hr"
    },
    {
    	id: "PREVIOUS_6_HRS",
    	value: 15,
    	name: "Previous 6 hrs"
    },
    {
    	id: "PREVIOUS_12_HRS",
    	value: 20,
    	name: "Previous 12 hrs"
    },
    {
        id: "PREVIOUS_DAY",
        value: 25,
        name: "Previous day"
    },
    {
        id: "PREVIOUS_WEEK",
        value: 30,
        name: "Previous week"
    },
    {
        id: "PREVIOUS_4WEEKS",
        value: 35,
        name: "Previous 4 weeks"
    },
    {
        id: "PREVIOUS_MONTH",
        value: 40,
        name: "Previous month"
    },
    {
        id: "PREVIOUS_YEAR",
        value: 45,
        name: "Previous year"
    },
    {
        id: "YESTERDAY",
        value: 50,
        name: "Yesterday"
    },
    {
        id: "LAST_WEEK",
        value: 55,
        name: "Last week"
    },
    {
        id: "LAST_MONTH",
        value: 60,
        name: "Last month"
    },
    {
        id: "LAST_YEAR",
        value: 65,
        name: "Last year"
    },
    {
        id: "DAY_TO_DATE",
        value: 70,
        name: "Day to date"
    },
    {
        id: "WEEK_TO_DATE",
        value: 75,
        name: "Week to date"
    },
    {
        id: "MONTH_TO_DATE",
        value: 80,
        name: "Month to date"
    },
    {
        id: "YEAR_TO_DATE",
        value: 85,
        name: "Year to date"
    },
    {
        id: "FIXED_TO_FIXED",
        value: 90,
        name: "Custom time period"
    },
    {
        id: "FIXED_TO_NOW",
        value: 95,
        name: "Custom time up to now"
    },
    {
        id: "INCEPTION_TO_FIXED",
        value: 100,
        name: "First value up to custom time"
    },
    {
        id: "INCEPTION_TO_NOW",
        value: 105,
        name: "First value up to now"
    }
];

TimePresetPicker.populatePresetPicker = function(picker) {
    if (!picker) return;
    var values = TimePresetPicker.presetValues.slice();
    
    if (picker instanceof $) {
        // jquery
        if (picker.children().length) return;
        
        for (var i = 0; i < values.length; i++) {
            var option = $('<option>');
            option.attr('value', values[i].id);
            option.text(values[i].name);
            picker.append(option);
        }
    } else if (picker.store && picker.store.data && !picker.store.data.length) {
        // dojo
        picker.set('store', new DstoreAdapter(new Memory({data: values}))); 
    }
};

/**
 * @param {string} preset - Preset to use for calculation
 */
TimePresetPicker.calculatePeriod = function(preset) {
    var to = moment();
    var from = moment(to);
    
    switch(preset) {
    case "PREVIOUS_1_HR":
    	from.subtract(1, 'hours');
    	break;
    case "PREVIOUS_6_HRS":
    	from.subtract(6, 'hours');
    	break;
    case "PREVIOUS_12_HRS":
    	from.subtract(12, 'hours');
    	break;
    case "PREVIOUS_DAY":
        from.subtract(1, 'days');
        break;
    case "PREVIOUS_WEEK":
        from.subtract(1, 'weeks');
        break;
    case "PREVIOUS_4WEEKS":
        from.subtract(4, 'weeks');
        break;
    case "PREVIOUS_MONTH":
        from.subtract(1, 'months');
        break;
    case "PREVIOUS_YEAR":
        from.subtract(1, 'years');
        break;
    case "YESTERDAY":
        to.hours(0).minutes(0).seconds(0).milliseconds(0);
        from = moment(to).subtract(1, 'days');
        break;
    case "LAST_WEEK":
        to.weekday(0).hours(0).minutes(0).seconds(0).milliseconds(0);
        from = moment(to).subtract(1, 'weeks');
        break;
    case "LAST_MONTH":
        to.date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
        from = moment(to).subtract(1, 'months');
        break;
    case "LAST_YEAR":
        to.dayOfYear(1).hours(0).minutes(0).seconds(0).milliseconds(0);
        from = moment(to).subtract(1, 'years');
        break;
    case "DAY_TO_DATE":
        from.hours(0).minutes(0).seconds(0).milliseconds(0);
        break;
    case "WEEK_TO_DATE":
        from.weekday(0).hours(0).minutes(0).seconds(0).milliseconds(0);
        break;
    case "MONTH_TO_DATE":
        from.date(1).hours(0).minutes(0).seconds(0).milliseconds(0);
        break;
    case "YEAR_TO_DATE":
        from.dayOfYear(1).hours(0).minutes(0).seconds(0).milliseconds(0);
        break;
    case "FIXED_TO_FIXED":
        from = null;
        to = null;
        break;
    case "FIXED_TO_NOW":
        from = null;
        break;
    case "INCEPTION_TO_FIXED":
        from = moment(0);
        to = null;
        break;
    case "INCEPTION_TO_NOW":
        from = moment(0);
        break;
    default:
        from = null;
        to = null;
        break;
    }
    
    return {from: from, to: to};
};

return TimePresetPicker;

}); // define
