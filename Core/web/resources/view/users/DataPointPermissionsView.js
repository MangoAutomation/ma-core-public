/**
 * Copyright (C) 2015 Infinite Automation. All rights reserved.
 * @author Terry Packer
 */

define(['jquery', 'view/BaseUIComponent', 'dstore/Rest', 'dstore/RequestFixed', 'dojo/store/Memory', 'dstore/Memory',
        'dojo/data/ObjectStore', 'dijit/form/Select', 'dijit/form/FilteringSelect', 'dstore/legacy/DstoreAdapter', 
        'dojo/_base/declare', 'dgrid/OnDemandGrid', 'dgrid/Editor', 'dgrid/extensions/ColumnResizer',
        'dgrid/extensions/DijitRegistry'], 
		function($, BaseUIComponent, Rest, Request, DojoMemory, Memory, ObjectStore,
				Select, FilteringSelect, DstoreAdapter, declare, OnDemandGrid, Editor, ColumnResizer, DijitRegistry){
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
			enabled: 'or',
 			xid: "*",
 			name: "*",
 			deviceName: "*",
 			dataSourceName: "*",
 			setPermission: "*",
 			readPermission: "*",
 		},{
			id: 1,
			enabled: 'disabled',
 			xid: "*",
 			name: "*",
 			deviceName: "*",
 			dataSourceName: "*",
 			setPermission: "*",
 			readPermission: "*",
 		},{
			id: 2,
			enabled: 'disabled',
 			xid: "*",
 			name: "*",
 			deviceName: "*",
 			dataSourceName: "*",
 			setPermission: "*",
 			readPermission: "*",
 		},{
			id: 3,
			enabled: 'disabled',
 			xid: "*",
 			name: "*",
 			deviceName: "*",
 			dataSourceName: "*",
 			setPermission: "*",
 			readPermission: "*",
 		}]
	});
	
	var filterOptionsData = [
					{ id: 'disabled', name: 'Disabled', order: 1 },
					{ id: 'and', name: 'And', order: 2 },
					{ id: 'or', name: 'Or', order: 3}
				];
	var filterOptionsStore = new DojoMemory({ data: filterOptionsData });
	var filterOptionsDataStore = new ObjectStore({
		objectStore: filterOptionsStore,
		labelProperty: 'name'
	});

	var thisView = this;
	this.filterGrid = new (declare([OnDemandGrid, Editor, ColumnResizer, DijitRegistry]))({
	    collection: this.filterStore,
	    columns: {
	    	enabled: {
	    		label: 'Filter State',
	    		editor: Select,
	    		editorArgs: {
	    			store: filterOptionsDataStore,
	    			sortByLabel: false
	    		},
	    		editOn: 'click',
	    		autoSave: true,
	        	sortable: false,
	        	renderCell: function(object, value, node, options) {
	        		$(node).text(filterOptionsStore.get(value).name);
	        	}
	    	},
	        xid: {
	        	label: this.tr('filter.byDataPointXid'),
	        	renderHeaderCell: function(node){
	        		$(node).text(this.label);
	        		var cb = $('<input />', { title: 'Check to enable', type: 'checkbox', id: 'cb-xid', value: name, style: 'margin-left: 5px', checked: 'checked'});
	        		cb.change(thisView.filterAttributeChanged.bind(thisView));
	        		cb.appendTo(node);
	        		return;
	        	},
	        	canEdit: function(){
	        		return $('#cb-' + this.field).is(':checked');
	        	},
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true,
	        	sortable: false,
	        },
	        name: {
	        	label: this.tr('filter.byDataPointName'),
	        	renderHeaderCell: function(node){
	        		$(node).text(this.label);
	        		var cb = $('<input />', { title: 'Check to enable', type: 'checkbox', id: 'cb-name', value: name, style: 'margin-left: 5px'});
	        		cb.change(thisView.filterAttributeChanged.bind(thisView));
	        		cb.appendTo(node);
	        		return;
	        	},
	        	canEdit: function(){
	        		return $('#cb-' + this.field).is(':checked');
	        	},
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true,
	        	sortable: false,
	        },
	        deviceName: {
	        	label: this.tr('filter.byDeviceName'),
	        	renderHeaderCell: function(node){
	        		$(node).text(this.label);
	        		var cb = $('<input />', { title: 'Check to enable', type: 'checkbox', id: 'cb-deviceName', value: name, style: 'margin-left: 5px'});
	        		cb.change(thisView.filterAttributeChanged.bind(thisView));
	        		cb.appendTo(node);
	        		return;
	        	},
	        	canEdit: function(){
	        		return $('#cb-' + this.field).is(':checked');
	        	},
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true,
	        	sortable: false,
	        },
	        dataSourceName: {
	            label: this.tr('filter.byDataSourceName'),
	            renderHeaderCell: function(node){
	        		$(node).text(this.label);
	        		var cb = $('<input />', { title: 'Check to enable', type: 'checkbox', id: 'cb-dataSourceName', value: name, style: 'margin-left: 5px'});
	        		cb.change(thisView.filterAttributeChanged.bind(thisView));
	        		cb.appendTo(node);
	        		return;
	        	},
	        	canEdit: function(){
	        		return $('#cb-' + this.field).is(':checked');
	        	},
	            editor: 'text',
	        	editOn: 'click',
	        	autoSave: true,
	        	sortable: false,
	        },
	        setPermission: {
	        	label: this.tr('filter.bySetPermissions'),
	        	renderHeaderCell: function(node){
	        		$(node).text(this.label);
	        		var cb = $('<input />', { title: 'Check to enable', type: 'checkbox', id: 'cb-setPermission', value: name, style: 'margin-left: 5px'});
	        		cb.change(thisView.filterAttributeChanged.bind(thisView));
	        		cb.appendTo(node);
	        		return;
	        	},
	        	canEdit: function(){
	        		return $('#cb-' + this.field).is(':checked');
	        	},
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true,
	        	sortable: false,
	        },
	        readPermission: {
	        	label: this.tr('filter.byReadPermissions'),
	        	renderHeaderCell: function(node){
	        		$(node).text(this.label);
	        		var cb = $('<input />', { title: 'Check to enable', type: 'checkbox', id: 'cb-readPermission', value: name, style: 'margin-left: 5px'});
	        		cb.change(thisView.filterAttributeChanged.bind(thisView));
	        		cb.appendTo(node);
	        		return;
	        	},
	        	canEdit: function(){
	        		return $('#cb-' + this.field).is(':checked');
	        	},
	        	editor: 'text',
	        	editOn: 'click',
	        	autoSave: true,
	        	sortable: false,
	        },
	    }	    
	}, 'filter-grid');
	
	this.filterGrid.on('dgrid-datachange', this.filterChanged.bind(this));
	
	this.baseURL = '/rest/v1/data-points';
	this.pointsStore = new (declare([Rest, Request]))({
	    target: this.baseURL,
	    idProperty: 'xid'
	});
//	this.pointsStore = new Request({
//	    target: this.baseURL,
//	    idProperty: 'xid'
//	});
	
	this.sortedPointsStore = this.pointsStore.sort([{property: 'dataSourceXid', descending: true}]);
	
	this.pointsGrid = new (declare([OnDemandGrid, Editor, ColumnResizer, DijitRegistry]))({
	    collection: this.sortedPointsStore,
	    columns: {
	        xid: {
	        	label: this.tr('filter.byDataPointXid'),
	        },
	        name: {
	        	label: this.tr('filter.byDataPointName'),
	        },
	        deviceName: {
	        	label: this.tr('filter.byDeviceName'),
	        },
	        dataSourceName: {
	            label: this.tr('filter.byDataSourceName'),
	        },
	        setPermission:{
	        	label: this.tr('filter.bySetPermissions'),
	        	renderCell: function(object, value, node, options){
	        		if(value === null)
	        			$(node).html('<b>null</b>');
	        		else
	        			$(node).text(value);
	        	},
	        	//Inline editing when we are confident the data 
	        	// point PUT REST endpoint is complete
	        	//editor: 'text',
	        	//editOn: 'dblclick',
	        	//autoSave: true
	        	
	        },
	        readPermission: {
	        	label: this.tr('filter.byReadPermissions'),
	        	renderCell: function(object, value, node, options){
	        		if(value === null)
	        			$(node).html('<b>null</b>');
	        		else
	        			$(node).text(value);
	        	},
	        	//Inline editing when we are confident the data 
	        	// point PUT REST endpoint is complete
	        	//editor: 'text',
	        	//editOn: 'dblclick',
	        	//autoSave: true,
	        },
	    },
	}, 'points-permissions-grid');
	var self = this;
	this.pointsGrid.on('dgrid-error', function(event){
		self.showMessage(event.error.message, 'error');
	});
	
	//Setup Apply Read Permission
	$('#applyReadPermission').on('click', {type: 'read'}, this.applyPermissions.bind(this));
	//Setup Apply Set Permission
	$('#applySetPermission').on('click', {type: 'set'}, this.applyPermissions.bind(this));
	
	//Setup Clear Read Permission
	$('#clearReadPermission').on('click', {type: 'read'}, this.clearPermissions.bind(this));
	//Setup Clear Set Permission
	$('#clearSetPermission').on('click', {type: 'set'}, this.clearPermissions.bind(this));

	//Setup the Permissions viewer
	$('#setPermissionsViewer').on('click', {inputNode: $('#setPermissions')}, this.showPermissionList.bind(this));
	$('#readPermissionsViewer').on('click', {inputNode: $('#readPermissions')}, this.showPermissionList.bind(this));

	
};

/**
 * 
 */
DataPointPermissionsView.prototype.filterAttributeChanged = function(event){
	this.filterChanged(); //Trigger re-load
};

/**
 * Filter has been modified
 */
DataPointPermissionsView.prototype.filterChanged = function(event){
	//Set to not use update if no event
	var rowId = -1;
	var columnId;	
	var cellValue; 
	if(typeof event != 'undefined'){
		//Get new info
		columnId = event.cell.column.id;
		rowId = event.cell.row.id;
		cellValue = event.value; 
	}
	
	//Get all active filters
	var filters = this.filterStore.filter();
	var totalFilter = null;
	var my = this;
	//Build the total filter
	filters.forEach(function(filter){
		if(filter.id == rowId)
			filter[columnId] = cellValue; //Update with new info from event
		for(var prop in filter){
			//Don't add extra info
			if((prop != 'enabled')&&(prop != 'id')&&(filter.enabled !== 'disabled')){
				//Only use enabled filter properties
				if($('#cb-' + prop).is(':checked') === true){
					if(totalFilter === null){
						totalFilter = new my.pointsStore.Filter().match(prop, filter[prop]);
					}else{
						//totalFilter[prop] = new Regex(filter[prop]);
						var newFilter = new my.pointsStore.Filter().match(prop, filter[prop]);
						if(filter.enabled === 'or')
							totalFilter = my.pointsStore.Filter().or(totalFilter, newFilter);
						else if(filter.enabled === 'and')
							totalFilter = my.pointsStore.Filter().and(totalFilter, newFilter);
					}
				}
			}
		}
	});
	
	//Just in case we don't have any criteria
	if(totalFilter === null)
		 totalFilter = new this.pointsStore.Filter();
	
	//console.log(totalFilter);
	
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
			//Disable buttons
			this.disableButtons();
			this.api.applyBulkPointReadPermissions(permissions, this.pointsStore._renderFilterParams(this.currentFilter)[0]).done(function(appliedCount){
				self.showSuccess(self.tr('permissions.bulkPermissionsApplied', appliedCount));
				self.pointsGrid.refresh();
				//Enable buttons
				self.enableButtons();
			}).fail(function(error){
				self.enableButtons();
				self.showError(error);
			});
			break;
		case 'set':
			permissions = $('#setPermissions').val();
			if(permissions === ''){
				$('#setPermissions').notify(this.tr('validate.invalidValue'));
				return;
			}
			//Disable buttons
			this.disableButtons();
			this.api.applyBulkPointSetPermissions(permissions, this.pointsStore._renderFilterParams(this.currentFilter)[0]).done(function(appliedCount){
				self.showSuccess(self.tr('permissions.bulkPermissionsApplied', appliedCount));
				self.pointsGrid.refresh();
				//Enable buttons
				self.enableButtons();
			}).fail(function(error){
				self.enableButtons();
				self.showError(error);
			});
			break;
		}

	}
};

DataPointPermissionsView.prototype.clearPermissions = function(event){
	//Get current filter
	if(typeof event.data.type != 'undefined'){
		
		if(!confirm(this.tr('permissions.bulkClearConfirm', event.data.type)))
			return;
		
		var permissions;
		//Do API Call
		var self = this;
		switch(event.data.type){
		case 'read':
			//Disable buttons
			this.disableButtons();
			this.api.clearBulkPointReadPermissions(this.pointsStore._renderFilterParams(this.currentFilter)[0]).done(function(appliedCount){
				self.showSuccess(self.tr('permissions.bulkPermissionsCleared', appliedCount));
				self.pointsGrid.refresh();
				//Enable buttons
				self.enableButtons();
			}).fail(function(error){
				self.enableButtons();
				self.showError(error);
			});
			break;
		case 'set':
			//Disable buttons
			this.disableButtons();
			this.api.clearBulkPointSetPermissions(this.pointsStore._renderFilterParams(this.currentFilter)[0]).done(function(appliedCount){
				self.showSuccess(self.tr('permissions.bulkPermissionsCleared', appliedCount));
				self.pointsGrid.refresh();
				//Enable buttons
				self.enableButtons();
			}).fail(function(error){
				self.enableButtons();
				self.showError(error);
			});
			break;
		}

	}
};

/**
 * Disable all the edit buttons
 */
DataPointPermissionsView.prototype.disableButtons = function(){
	//Show loading
	$('#permissionAction').css('display', 'block');
	$('#permissionButtons').hide();
	
	$('#applyReadPermission').prop('disabled', true);
	$('#clearReadPermission').prop('disabled', true);
	$('#applySetPermission').prop('disabled', true);
	$('#clearSetPermission').prop('disabled', true);
};
/**
 * Enable all the edit buttons
 */
DataPointPermissionsView.prototype.enableButtons = function(){
	$('#applyReadPermission').prop('disabled', false);
	$('#clearReadPermission').prop('disabled', false);
	$('#applySetPermission').prop('disabled', false);
	$('#clearSetPermission').prop('disabled', false);
	//Hide loading
	$('#permissionAction').css('display', 'none');
	$('#permissionButtons').show();
};

return DataPointPermissionsView;
	
});
