/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent', 'dstore/Rest', 'dstore/Memory',
        'dijit/form/filteringSelect', 'dstore/legacy/DstoreAdapter', 
        'dojo/_base/declare', 'dgrid/OnDemandGrid', 'dgrid/Editor', 'dgrid/extensions/ColumnResizer'], 
		function($, BaseUIComponent, Rest, Memory,
				FilteringSelect, DstoreAdapter, declare, OnDemandGrid, Editor, ColumnResizer){
"use strict";

function DataPointPermissionsView(){
	
	BaseUIComponent.apply(this, arguments);
	this.currentFilter = {};
}

DataPointPermissionsView.prototype = Object.create(BaseUIComponent.prototype);

DataPointPermissionsView.prototype.pointsStore = null;
DataPointPermissionsView.prototype.pointsGrid = null;
DataPointPermissionsView.prototype.filterPicker = null;
DataPointPermissionsView.prototype.currentFilter = null;
DataPointPermissionsView.prototype.defaultURL = null;

DataPointPermissionsView.prototype.setupView = function(){
	
	this.filterStore = new Memory({
		data: [{
			id: 0,
			enabled: true,
 			xid: null,
 			name: null,
 			dataSourceXid: null,
 			setPermission: null,
 			readPermission: null,
 		},{
			id: 1,
			enabled: false,
 			xid: null,
 			name: null,
 			dataSourceXid: null,
 			setPermission: null,
 			readPermission: null,
 		},{
			id: 2,
			enabled: false,
 			xid: null,
 			name: null,
 			dataSourceXid: null,
 			setPermission: null,
 			readPermission: null,
 		}]
	});
	
	this.filterGrid = new (declare([OnDemandGrid, Editor, ColumnResizer]))({
	    collection: this.filterStore,
	    columns: {
	    	enabled: {
	    		label: 'Filter Enabled',
	    		editor: 'checkbox',
	    		editOn: 'click',
	    		autoSave: true
	    	},
	        xid: {
	        	label: this.tr('filter.byDataPointXid'),
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	        name: {
	        	lable: this.tr('filter.byDataPointName'),
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	        dataSourceXid: {
	            label: this.tr('filter.byDataSourceXid'),
	            editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	        setPermission:{
	        	label: this.tr('filter.bySetPermissions'),
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	        readPermission: {
	        	label: this.tr('filter.byReadPermissions'),
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	    }	    
	}, 'filter-grid');
	
	this.filterGrid.on('dgrid-datachange', this.filterChanged.bind(this));
	
	this.baseURL = '/rest/v1/data-points.json';
	this.pointsStore = new Rest({
	    target: this.baseURL,
	    idProperty: 'xid'
	});
	
	this.sortedPointsStore = this.pointsStore.sort([{property: 'dataSourceXid', descending: true}]);
	
	this.pointsGrid = new (declare([OnDemandGrid, Editor, ColumnResizer]))({
	    collection: this.sortedPointsStore,
	    columns: {
	        xid: this.tr('filter.byDataPointXid'),
	        name: this.tr('filter.byDataPointName'),
	        dataSourceXid: {
	            label: this.tr('filter.byDataSourceXid'),
	        },
	        setPermission:{
	        	label: this.tr('filter.bySetPermissions'),
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	        readPermission: {
	        	label: this.tr('filter.byReadPermissions'),
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true
	        },
	    },
	}, 'points-permissions-grid');
	var self = this;
	this.pointsGrid.on('dgrid-error', function(event){
		self.showMessage(event.error.message, 'error');
		self.pointsGrid.refresh();
	});
	
	//Setup Apply Read Permission
	$('#applyReadPermission').on('click', {type: 'read'}, this.applyPermissions.bind(this));
	//Setup Apply Set Permission
	$('#applySetPermission').on('click', {type: 'set'}, this.applyPermissions.bind(this));

};

DataPointPermissionsView.prototype.filterChanged = function(event){
	
	//Get new info
	var columnId = event.cell.column.id;
	var rowId = event.cell.row.id;
	var cellValue = event.value; 
	
	//Get all active filters
	var filters = this.filterStore.filter();
	var totalFilter = new this.pointsStore.Filter();
	
	//Build the total filter
	filters.forEach(function(filter){
		if(filter.id == rowId)
			filter[columnId] = cellValue; //Update with new info from event
		for(var prop in filter){
			//Don't add extra info
			if((prop != 'enabled')&&(prop != 'id')&&(filter.enabled === true)){
				if((filter[prop] != null)&&(filter[prop] != "")){
					//totalFilter[prop] = new Regex(filter[prop]);
					totalFilter = totalFilter.match(prop, filter[prop]);
				}
			}
		}
	});
	
	this.currentFilter = totalFilter;
	var filteredStore = this.pointsStore.filter(totalFilter);
	//Do filter on store
	this.pointsGrid.set('collection', filteredStore);
};

DataPointPermissionsView.prototype.clearFilter = function(){
	//Reset filter view
	//Clear out store filter
};

DataPointPermissionsView.prototype.applyPermissions = function(event){
	//Get current filter
	if(typeof event.data.type != 'undefined'){
		
		if(!confirm(this.tr('permissions.bulkApplyConfirm', event.data.type)))
			return;
		
		var permissions;
		//Do API Call
		var self = this;
		switch(event.data.type){
		case 'read':
			permissions = $('#readPermissions').val();
			if(permissions === ''){
				$('#readPermissions').notify(this.tr('validate.invalidValue'));
				return;
			}
			this.api.applyBulkPointReadPermissions(permissions, this.pointsStore._renderFilterParams(this.currentFilter)[0]).done(function(appliedCount){
				self.showSuccess(self.tr('permissions.bulkPermissionsApplied', appliedCount));
				self.pointsGrid.refresh();
			}).fail(this.showError);
			break;
		case 'set':
			permissions = $('#setPermissions').val();
			if(permissions === ''){
				$('#setPermissions').notify(this.tr('validate.invalidValue'));
				return;
			}
			this.api.applyBulkPointSetPermissions(permissions, this.pointsStore._renderFilterParams(this.currentFilter)[0]).done(function(appliedCount){
				self.showSuccess(self.tr('permissions.bulkPermissionsApplied', appliedCount));
				self.pointsGrid.refresh();
			}).fail(this.showError);
			break;
		}

	}
};


return DataPointPermissionsView;
	
});