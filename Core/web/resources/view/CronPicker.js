/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Jared Wiltshire
 */
 
 define(['jquery'
], function($) {
'use strict';

function CronPicker(options) {
    this._pattern = {};
    this.$input = $();
    this.$picker = $();
    
    $.extend(this, options);
    
    this.$input.on('change.set-property', this.inputChanged.bind(this));
    this.$picker.find(':input').on('change', this.pickerChanged.bind(this));
}

/**
 * The input to update when the picker is changed
 */
CronPicker.prototype.$input = null;
/**
 * The picker which updates the input
 */
CronPicker.prototype.$picker = null;

CronPicker.prototype.pickerChanged = function(event) {
    var $pickerInput = $(event.target);
    var name = $pickerInput.attr('name');
    var value = $pickerInput.val();
    
    // default to "every" if user unselects all options
    if (!value || !value.length) {
        value = ['*'];
        $pickerInput.val(value);
    }
    
    // only allow multiple selections of numbers, i.e. disallow multiple
    // selections which contain special characters
    var prevValue = this._pattern[name];
    if (value.length > 1 && value.length > prevValue.length ) {
        for (var i = 0; i < value.length; i++) {
            var val = value[i];
            if (val === '*' || val === '?' || val.indexOf('/') >= 0) {
                value = prevValue;
                $pickerInput.val(value);
                break;
            }
        }
    }
    
    // Either dayOfMonth or dayOfWeek must be ?
    if (name.indexOf('day') === 0) {
        var otherDay = name === 'dayOfMonth' ? 'dayOfWeek' : 'dayOfMonth';
        this.setPatternField(otherDay, value[0] === '?' ? ['*'] : ['?']);
    }
    
    this._pattern[name] = value;
    
    this.setInputValue();
    event.stopImmediatePropagation();
};

CronPicker.prototype.setPatternField = function(field, value) {
    this._pattern[field] = value;
    this.$picker.find('[name="' + field + '"]').val(value);
};

CronPicker.prototype.inputChanged = function(event, data) {
    if (data && data.fromCronPicker) return;
    
    var $input = $(event.target);
    this.parsePattern($input.val());
    
    this.setPickerValues();
};

CronPicker.prototype.parsePattern = function(pattern) {
    var valArray = pattern.split(' ');
    
    this._pattern.second = this.parseFieldPattern(valArray[0]);
    this._pattern.minute = this.parseFieldPattern(valArray[1]);
    this._pattern.hour = this.parseFieldPattern(valArray[2]);
    this._pattern.dayOfMonth = this.parseFieldPattern(valArray[3]);
    this._pattern.month = this.parseFieldPattern(valArray[4]);
    this._pattern.dayOfWeek = this.parseFieldPattern(valArray[5], ['?']);
};

/**
 * Find ranges like "4-7" and convert to ["4","5","6","7"]
 * Find combined values like "1,7" and convert to ["1", "7"]
 */
CronPicker.prototype.expandRanges = function(selections) {
    for (var i = 0; i < selections.length; i++) {
        var sel = selections[i];
        if (sel.indexOf(',') >= 0) {
            selections.splice.apply(selections, [i, 1].concat(sel.split(',')));
        }
        var match = selections[i].match(/(\d+)-(\d+)/);
        if (match) {
            selections.splice(i, 1);
            for (var j = parseInt(match[2], 10); j >= parseInt(match[1], 10); j--) {
                selections.splice(i, 0, '' + j);
            }
        }
    }
};

CronPicker.prototype.removeDuplicates = function(selections) {
    for (var i = 0; i < selections.length;) {
        var dupIndex = selections.indexOf(selections[i], i+1);
        if (dupIndex > i) {
            selections.splice(dupIndex, 1);
            continue;
        }
        i++;
    }
};

CronPicker.prototype.parseFieldPattern = function(pattern, defaultResult) {
    if (!pattern) return defaultResult || ['*'];
    
    var selections = pattern.split(',');
    this.expandRanges(selections);
    this.removeDuplicates(selections);
    this.sort(selections);
    
    return selections;
};

CronPicker.prototype.setPickerValues = function() {
    for (var name in this._pattern) {
        var $pickerInput = this.$picker.find('[name="' + name + '"]');
        $pickerInput.val(this._pattern[name]);
    }
};

CronPicker.prototype.sort = function(selections) {
    selections.sort(function(a, b) {
        var aNum = parseInt(a, 10);
        var bNum = parseInt(b, 10);
        if (isNaN(aNum) || isNaN(bNum)) {
            return -1;
        }
        return aNum - bNum;
    });
};

CronPicker.prototype.getField = function(field) {
    var value = this._pattern[field];
    this.expandRanges(value);
    this.removeDuplicates(value);
    this.sort(value);
    
    // convert consecutive selections into ranges, array must be sorted!
    for (var i = 0; i < value.length; i++) {
        var from = parseInt(value[i]);
        var to = null;
        
        if (isNaN(from)) continue;
        
        for (var j = i+1; j < value.length; j++) {
            var next = parseInt(value[j]);
            if (!isNaN(next) && next === from + j-i) {
                to = next;
            } else {
                break;
            }
        }
        if (to !== null) {
            value.splice(i, j-i, from + '-' + to);
        }
    }
    
    return value.join(',');
};

CronPicker.prototype.pattern = function() {
    var field = this.getField;
    var valArray = [this.getField('second'), this.getField('minute'), this.getField('hour'),
                    this.getField('dayOfMonth'), this.getField('month'), this.getField('dayOfWeek')];
    return valArray.join(' ');
};

CronPicker.prototype.setInputValue = function() {
    this.$input.val(this.pattern());
    this.$input.trigger('change', {fromCronPicker: true});
};

return CronPicker;

}); // define
