/**
 * Copyright (C) 2015 Infinite Automation Systems, Inc. All rights reserved.
 * http://infiniteautomation.com/
 * @author Terry Packer
 */

define(['jquery', 'dstore/Memory'], function($, Memory) {
'use strict';

function TimePresetStore() {
	Memory.apply(this, arguments);
}

TimePresetStore.prototype = Object.create(Memory.prototype);

TimePresetStore.prototype.data = [

        {id:'PREVIOUS_1_HR', text: 'Previous 1 hrs'},                         
        {id:'PREVIOUS_6_HRS', text: 'Previous 6 hrs'},                         
        {id:'PREVIOUS_12_HRS', text: 'Previous 12 hrs'},                         
        {id:'PREVIOUS_DAY', text: 'Previous day'},
        {id:'PREVIOUS_WEEK', text: 'Previous week'},
        {id:'PREVIOUS_4WEEKS', text: 'Previous 4 weeks'},
        {id:'PREVIOUS_MONTH', text: 'Previous month'},
        {id:'PREVIOUS_YEAR', text: 'Previous year'},
        {id:'YESTERDAY', text: 'Yesterday'},
        {id:'LAST_WEEK', text: 'Last week'},
        {id:'LAST_MONTH', text: 'Last month'},
        {id:'LAST_YEAR', text: 'Last year'},
        {id:'DAY_TO_DATE', text: 'Day to date'},
        {id:'WEEK_TO_DATE', text: 'Week to date'},
        {id:'MONTH_TO_DATE', text: 'Month to date'},
        {id:'YEAR_TO_DATE', text: 'Year to date'},
        {id:'FIXED_TO_FIXED', text: 'Custom time period'},
        {id:'FIXED_TO_NOW', text: 'Custom time up to now'},
        {id:'INCEPTION_TO_FIXED', text: 'First value up to custom time'},
        {id:'INCEPTION_TO_NOW', text: 'First value up to now'}
];


return TimePresetStore;

}); // define